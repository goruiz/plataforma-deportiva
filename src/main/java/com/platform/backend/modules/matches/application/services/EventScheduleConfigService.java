package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.matches.application.iservices.IEventScheduleConfigService;
import com.platform.backend.modules.matches.application.engine.MatchDraft;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.modules.matches.domain.exception.ChampionshipError;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventScheduleConfigService implements IEventScheduleConfigService {

    private static final int MAX_SEARCH_YEARS = 5;
    private static final int MINUTES_PER_DAY = 24 * 60;

    private final IEventScheduleConfigRepository configRepository;
    private final IEventRepository eventRepository;
    private final ITeamsEventsRepository teamsEventsRepository;
    private final IMatchRepository matchRepository;
    private final IHolidaysService holidaysService;
    private final IMatchesService matchesService;
    private final MatchMapper matchMapper;

    @Override
    public EventScheduleConfigResponse saveConfig(UUID eventId, SaveScheduleConfigRequest request) {
        eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        EventScheduleConfigEntity config = configRepository.findActiveByEventId(eventId)
                .orElseGet(EventScheduleConfigEntity::new);

        config.setEventId(eventId);
        config.setPlayDays(String.join(",", request.getPlayDays()));
        config.setStartTime(LocalTime.parse(request.getStartTime()));
        config.setMatchDurationMinutes(request.getMatchDurationMinutes());
        config.setBreakBetweenHalvesMinutes(request.getBreakBetweenHalvesMinutes());
        config.setBreakBetweenMatchesMinutes(request.getBreakBetweenMatchesMinutes());
        config.setCourtId(request.getCourtId());

        if (request.getBlockedDates() != null && !request.getBlockedDates().isEmpty()) {
            config.setBlockedDates(
                request.getBlockedDates().stream()
                    .map(LocalDate::toString)
                    .collect(Collectors.joining(","))
            );
        } else {
            config.setBlockedDates(null);
        }

        return toResponse(configRepository.save(config));
    }

    @Override
    public EventScheduleConfigResponse getConfig(UUID eventId) {
        return configRepository.findActiveByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Schedule config not found for event: " + eventId));
    }

    /**
     * Schedules every pairing that has not been played yet, round after round, on the first valid day for both
     * teams. Calling it again is safe: the matches still waiting to be played are replaced, the ones already
     * played (or called off) are kept and their pairings are not scheduled again.
     */
    @Override
    public GenerateMatchesResponse generateMatches(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        EventScheduleConfigEntity config = configRepository.findActiveByEventId(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule config not found. Save a config first."));

        List<UUID> teamIds = findRegisteredTeamIds(eventId);

        // A match whose time is over counts as played even if the periodic job has not marked it yet: it must be
        // kept, not replaced by a new one.
        matchesService.finishEndedMatches(eventId);

        List<MatchesEntity> existingMatches = matchRepository.findAllActiveByEventId(eventId);
        List<MatchesEntity> pendingMatches = existingMatches.stream().filter(this::isPending).toList();
        List<MatchesEntity> settledMatches = existingMatches.stream().filter(match -> !isPending(match)).toList();

        LocalDate startDate = firstSchedulableDate(event);
        ScheduleRules rules = buildRules(config, startDate);
        List<UUID[]> pairings = withoutSettledPairings(buildRoundRobinPairings(teamIds), settledMatches);

        discardMatches(pendingMatches);
        List<MatchResponse> created = scheduleMatches(eventId, config, pairings, rules, settledMatches);

        return new GenerateMatchesResponse(created, buildWarning(event.getEndDate(), latestDateOf(created)));
    }

    // ------------------------------------------------------------------ generation

    private List<UUID> findRegisteredTeamIds(UUID eventId) {
        List<UUID> teamIds = teamsEventsRepository.findAllActiveByEventId(eventId)
                .stream()
                .map(te -> te.getTeam().getId())
                .toList();

        if (teamIds.size() < 2) {
            throw new IllegalStateException(
                "Cannot generate matches: the event has " + teamIds.size() +
                " team(s). At least 2 teams are required.");
        }
        return teamIds;
    }

    /** A match that hasn't been played yet (and has no result), so a new calendar can take its place. */
    private boolean isPending(MatchesEntity match) {
        return MatchStatus.isPending(match.getStatus()) && match.getHomeScore() == null && match.getAwayScore() == null;
    }

    /** Matches are never placed in the past: an event that already started is scheduled from today on. */
    private LocalDate firstSchedulableDate(EventsEntity event) {
        LocalDate today = LocalDate.now();
        LocalDate eventStartDate = event.getStartDate();
        return eventStartDate != null && eventStartDate.isAfter(today) ? eventStartDate : today;
    }

    private ScheduleRules buildRules(EventScheduleConfigEntity config, LocalDate startDate) {
        LocalDate searchLimit = startDate.plusYears(MAX_SEARCH_YEARS);

        // The event's own blocked dates plus every holiday the administrator registered.
        Set<LocalDate> blockedDates = new HashSet<>(parseBlockedDates(config.getBlockedDates()));
        blockedDates.addAll(holidaysService.getNonPlayDatesBetween(startDate, searchLimit));

        return new ScheduleRules(
                parsePlayDays(config.getPlayDays()),
                blockedDates,
                config.getStartTime(),
                config.getMatchDurationMinutes(),
                config.getMatchDurationMinutes() + config.getBreakBetweenMatchesMinutes(),
                startDate,
                searchLimit
        );
    }

    private List<UUID[]> withoutSettledPairings(List<UUID[]> pairings, List<MatchesEntity> settledMatches) {
        Set<List<UUID>> settledPairs = settledMatches.stream()
                .map(match -> pairKey(match.getHomeTeamId(), match.getAwayTeamId()))
                .collect(Collectors.toSet());

        return pairings.stream()
                .filter(pairing -> !settledPairs.contains(pairKey(pairing[0], pairing[1])))
                .toList();
    }

    /** The same key for "A vs B" and "B vs A". */
    private static List<UUID> pairKey(UUID first, UUID second) {
        return first.compareTo(second) <= 0 ? List.of(first, second) : List.of(second, first);
    }

    private void discardMatches(List<MatchesEntity> matches) {
        LocalDateTime now = LocalDateTime.now();
        for (MatchesEntity match : matches) {
            match.setDeletedAt(now);
            matchRepository.delete(match);
        }
    }

    private List<MatchResponse> scheduleMatches(UUID eventId, EventScheduleConfigEntity config, List<UUID[]> pairings,
                                                ScheduleRules rules, List<MatchesEntity> settledMatches) {
        DayOccupancy occupancy = occupancyOf(settledMatches, rules.startDate());
        List<MatchResponse> created = new ArrayList<>();

        for (UUID[] pairing : pairings) {
            created.add(createScheduledMatch(eventId, config, pairing, rules, occupancy, match -> { }));
        }
        return created;
    }

    /**
     * Creates the matches a championship phase needs, each on the first free slot of the event's calendar (the same
     * rules as {@link #generateMatches}: play days, no holidays, nobody plays twice in a day). The matches already
     * in the event keep their slots; nothing is replaced.
     */
    @Override
    public List<MatchResponse> scheduleDrafts(UUID eventId, List<MatchDraft> drafts) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        EventScheduleConfigEntity config = configRepository.findActiveByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(ChampionshipError.SCHEDULE_CONFIG_REQUIRED.name()));

        ScheduleRules rules = buildRules(config, firstSchedulableDate(event));
        DayOccupancy occupancy = occupancyOf(matchRepository.findAllActiveByEventId(eventId), rules.startDate());
        List<MatchResponse> created = new ArrayList<>();

        for (MatchDraft draft : drafts) {
            UUID[] pairing = {draft.homeTeamId(), draft.awayTeamId()};
            created.add(createScheduledMatch(eventId, config, pairing, rules, occupancy, match -> tagWithChampionshipPlace(match, draft)));
        }
        return created;
    }

    private MatchResponse createScheduledMatch(UUID eventId, EventScheduleConfigEntity config, UUID[] pairing,
                                               ScheduleRules rules, DayOccupancy occupancy, Consumer<MatchesEntity> tagger) {
        LocalDateTime kickOff = findFreeSlot(pairing[0], pairing[1], rules, occupancy)
                .orElseThrow(() -> new IllegalStateException(
                    "Could not schedule all matches within " + MAX_SEARCH_YEARS +
                    " years from " + rules.startDate() + ". Check your play days configuration."));

        MatchesEntity match = newScheduledMatch(eventId, config, pairing, kickOff);
        tagger.accept(match);
        MatchResponse response = matchMapper.toResponse(matchRepository.save(match));
        response.setEndsAt(kickOff.plusMinutes(rules.slotDurationMinutes()));
        occupancy.add(kickOff.toLocalDate(), pairing[0], pairing[1]);
        return response;
    }

    private void tagWithChampionshipPlace(MatchesEntity match, MatchDraft draft) {
        match.setStage(draft.stage());
        match.setRoundNumber(draft.roundNumber());
        match.setGroupNumber(draft.groupNumber());
        match.setTieNumber(draft.tieNumber());
        match.setLegNumber(draft.legNumber());
    }

    /** The matches that were already played or called off occupy their days too: a team can't play twice in one day. */
    private DayOccupancy occupancyOf(List<MatchesEntity> settledMatches, LocalDate startDate) {
        DayOccupancy occupancy = new DayOccupancy();
        settledMatches.stream()
                .filter(match -> !match.getMatchDate().toLocalDate().isBefore(startDate))
                .forEach(match -> occupancy.add(match.getMatchDate().toLocalDate(), match.getHomeTeamId(), match.getAwayTeamId()));
        return occupancy;
    }

    /** The first day (from the start) that is playable, has both teams free and still has room for another match. */
    private Optional<LocalDateTime> findFreeSlot(UUID homeId, UUID awayId, ScheduleRules rules, DayOccupancy occupancy) {
        for (LocalDate day = rules.startDate(); !day.isAfter(rules.searchLimit()); day = day.plusDays(1)) {
            if (!rules.isPlayable(day) || !occupancy.areBothFree(day, homeId, awayId)) {
                continue;
            }
            Optional<LocalTime> kickOffTime = rules.kickOffTime(occupancy.matchCountOn(day));
            if (kickOffTime.isPresent()) {
                return Optional.of(LocalDateTime.of(day, kickOffTime.get()));
            }
        }
        return Optional.empty();
    }

    private MatchesEntity newScheduledMatch(UUID eventId, EventScheduleConfigEntity config, UUID[] pairing, LocalDateTime kickOff) {
        MatchesEntity match = new MatchesEntity();
        match.setEventId(eventId);
        match.setHomeTeamId(pairing[0]);
        match.setAwayTeamId(pairing[1]);
        match.setMatchDate(kickOff);
        match.setStatus(MatchStatus.SCHEDULED.name());
        if (config.getCourtId() != null) {
            match.setCourtId(config.getCourtId());
        }
        return match;
    }

    private LocalDate latestDateOf(List<MatchResponse> matches) {
        return matches.stream()
                .map(match -> match.getMatchDate().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    // ------------------------------------------------------------------ helpers

    private String buildWarning(LocalDate eventEndDate, LocalDate latestMatchDate) {
        if (eventEndDate == null || latestMatchDate == null) return null;
        if (!latestMatchDate.isAfter(eventEndDate)) return null;
        return "Some matches were scheduled beyond the event end date (" + eventEndDate + "). " +
               "The last match is on " + latestMatchDate + ". " +
               "Consider updating the event end date to " + latestMatchDate + " or later.";
    }

    private List<UUID[]> buildRoundRobinPairings(List<UUID> teams) {
        List<UUID> rotation = new ArrayList<>(teams);
        if (rotation.size() % 2 != 0) {
            rotation.add(null); // null = bye (skipped)
        }
        int n = rotation.size();
        int numRounds = n - 1;
        List<UUID[]> pairings = new ArrayList<>();

        for (int round = 0; round < numRounds; round++) {
            for (int i = 0; i < n / 2; i++) {
                UUID home = rotation.get(i);
                UUID away = rotation.get(n - 1 - i);
                if (home != null && away != null) {
                    pairings.add(new UUID[]{home, away});
                }
            }
            // Rotate: keep rotation[0] fixed, insert last element at position 1
            UUID last = rotation.remove(n - 1);
            rotation.add(1, last);
        }
        return pairings;
    }

    private Set<DayOfWeek> parsePlayDays(String playDays) {
        Set<DayOfWeek> days = new LinkedHashSet<>();
        List<String> invalid = new ArrayList<>();
        for (String day : playDays.split(",")) {
            String value = day.trim().toUpperCase();
            try {
                days.add(DayOfWeek.valueOf(value));
            } catch (IllegalArgumentException e) {
                invalid.add(value);
            }
        }
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid play day value(s): " + invalid + ". " +
                "Use English day names: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY.");
        }
        if (days.isEmpty()) {
            throw new IllegalArgumentException("At least one play day must be configured.");
        }
        return days;
    }

    private Set<LocalDate> parseBlockedDates(String blockedDates) {
        if (blockedDates == null || blockedDates.isBlank()) return Collections.emptySet();
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (String d : blockedDates.split(",")) {
            String value = d.trim();
            if (!value.isEmpty()) {
                dates.add(LocalDate.parse(value));
            }
        }
        return dates;
    }

    private EventScheduleConfigResponse toResponse(EventScheduleConfigEntity entity) {
        EventScheduleConfigResponse r = new EventScheduleConfigResponse();
        r.setId(entity.getId());
        r.setEventId(entity.getEventId());
        r.setPlayDays(Arrays.asList(entity.getPlayDays().split(",")));
        r.setStartTime(entity.getStartTime().toString());
        r.setMatchDurationMinutes(entity.getMatchDurationMinutes());
        r.setBreakBetweenHalvesMinutes(entity.getBreakBetweenHalvesMinutes());
        r.setBreakBetweenMatchesMinutes(entity.getBreakBetweenMatchesMinutes());
        r.setCourtId(entity.getCourtId());
        r.setBlockedDates(parseBlockedDates(entity.getBlockedDates()).stream().toList());
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        return r;
    }

    // ------------------------------------------------------------------ scheduling helpers

    /** Which days a match may be played on, and at what time each match of a day starts. */
    private record ScheduleRules(Set<DayOfWeek> playDays, Set<LocalDate> blockedDates, LocalTime firstKickOff,
                                 int matchDurationMinutes, int slotDurationMinutes,
                                 LocalDate startDate, LocalDate searchLimit) {

        boolean isPlayable(LocalDate day) {
            return playDays.contains(day.getDayOfWeek()) && !blockedDates.contains(day);
        }

        /** Kick-off of the match that goes after {@code matchesAlreadyOnTheDay}; empty when it would end after midnight. */
        Optional<LocalTime> kickOffTime(int matchesAlreadyOnTheDay) {
            long startMinute = firstKickOff.toSecondOfDay() / 60 + (long) matchesAlreadyOnTheDay * slotDurationMinutes;
            if (startMinute + matchDurationMinutes > MINUTES_PER_DAY) {
                return Optional.empty();
            }
            return Optional.of(LocalTime.of((int) (startMinute / 60), (int) (startMinute % 60)));
        }
    }

    /** The teams and the number of matches each day already holds. */
    private static final class DayOccupancy {
        private final Map<LocalDate, Set<UUID>> teamsByDay = new HashMap<>();
        private final Map<LocalDate, Integer> matchCountByDay = new HashMap<>();

        boolean areBothFree(LocalDate day, UUID homeId, UUID awayId) {
            Set<UUID> busyTeams = teamsByDay.getOrDefault(day, Collections.emptySet());
            return !busyTeams.contains(homeId) && !busyTeams.contains(awayId);
        }

        int matchCountOn(LocalDate day) {
            return matchCountByDay.getOrDefault(day, 0);
        }

        void add(LocalDate day, UUID homeId, UUID awayId) {
            Set<UUID> busyTeams = teamsByDay.computeIfAbsent(day, key -> new HashSet<>());
            busyTeams.add(homeId);
            busyTeams.add(awayId);
            matchCountByDay.merge(day, 1, Integer::sum);
        }
    }
}
