package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsSportComplexesRepository;
import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.matches.application.iservices.IEventScheduleConfigService;
import com.platform.backend.modules.matches.application.engine.CalendarPlanner;
import com.platform.backend.modules.matches.application.engine.MatchDraft;
import com.platform.backend.modules.matches.application.engine.ScheduleRules;
import com.platform.backend.modules.matches.application.iservices.IMatchTimingService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.modules.matches.domain.exception.ChampionshipError;
import com.platform.backend.modules.matches.domain.exception.MatchError;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import com.platform.backend.shared.infraestructure.tenancy.TenantReferences;
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

    /** The order courts are tried in when they are tied: by name, and by id so the order never changes between runs. */
    private static final Comparator<CourtsEntity> COURT_ORDER =
            Comparator.comparing(CourtsEntity::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(CourtsEntity::getId);

    private final IEventScheduleConfigRepository configRepository;
    private final IEventRepository eventRepository;
    private final ITeamsEventsRepository teamsEventsRepository;
    private final IMatchRepository matchRepository;
    private final ICourtRepository courtRepository;
    private final IEventsSportComplexesRepository eventsSportComplexesRepository;
    private final IHolidaysService holidaysService;
    private final IMatchesService matchesService;
    private final IMatchTimingService matchTimingService;
    private final MatchSettingsReader settingsReader;
    private final MatchMapper matchMapper;

    @Override
    public EventScheduleConfigResponse saveConfig(UUID eventId, SaveScheduleConfigRequest request) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        CreatorAccess.requireAdminOrCreator(event.getCreatedBy());
        requireCourtIsVisible(request.getCourtId());

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

    // The court is a plain id: a restricted caller may only schedule on their own organization's courts.
    private void requireCourtIsVisible(UUID courtId) {
        TenantReferences.requireVisible(courtId, false, id -> courtRepository.findActiveById(id).isPresent(),
                () -> new FieldValidationException("courtId", MatchError.COURT_NOT_FOUND.name()));
    }

    @Override
    public EventScheduleConfigResponse getConfig(UUID eventId) {
        return configRepository.findActiveByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Schedule config not found for event: " + eventId));
    }

    /**
     * Schedules every pairing that has not been played yet, round after round, on the first valid day for both
     * teams, and spreads the matches over the courts of the complexes the event is played in (see
     * {@link CalendarPlanner}). Calling it again is safe: the matches still waiting to be played are replaced, the ones
     * already played (or called off) are kept and their pairings are not scheduled again.
     */
    @Override
    public GenerateMatchesResponse generateMatches(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        CreatorAccess.requireAdminOrCreator(event.getCreatedBy());

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
        ScheduleRules rules = buildRules(event, config, startDate);
        List<UUID[]> pairings = withoutSettledPairings(buildRoundRobinPairings(teamIds), settledMatches);

        discardMatches(pendingMatches);
        CalendarPlanner planner = planCalendar(event, config, rules, settledMatches);
        List<MatchResponse> created = scheduleMatches(eventId, pairings, rules, planner);

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

    private ScheduleRules buildRules(EventsEntity event, EventScheduleConfigEntity config, LocalDate startDate) {
        LocalDate searchLimit = startDate.plusYears(MAX_SEARCH_YEARS);

        // The event's own blocked dates plus every holiday its organization registered (each one has its own).
        Set<LocalDate> blockedDates = new HashSet<>(parseBlockedDates(config.getBlockedDates()));
        blockedDates.addAll(holidaysService.getNonPlayDatesBetween(event.getOrganizationId(), startDate, searchLimit));

        // A slot is what MatchTimingService says a match takes: its duration, the break after it and the wait the
        // organization's rules give every match before it starts.
        int waitMinutes = settingsReader.forEvent(event.getId()).matchWaitMinutes();
        return new ScheduleRules(
                parsePlayDays(config.getPlayDays()),
                blockedDates,
                config.getStartTime(),
                config.getMatchDurationMinutes(),
                config.getMatchDurationMinutes() + config.getBreakBetweenMatchesMinutes() + waitMinutes,
                startDate,
                searchLimit
        );
    }

    /**
     * The calendar as it already is: the matches of the event that stay keep their teams, courts and slots, and so do
     * the matches that other events hold in the courts this one uses.
     */
    private CalendarPlanner planCalendar(EventsEntity event, EventScheduleConfigEntity config, ScheduleRules rules,
                                         List<MatchesEntity> matchesThatStay) {
        List<UUID> courtIds = resolveCourtIds(event.getId(), config);
        CalendarPlanner planner = new CalendarPlanner(rules, courtIds);

        // Each match holds its court until it ends: its own slot, which an elimination that may need extra time makes longer.
        Map<UUID, LocalDateTime> endTimes = matchTimingService.getEndTimes(matchesThatStay);
        for (MatchesEntity match : matchesThatStay) {
            planner.addExistingMatch(match.getHomeTeamId(), match.getAwayTeamId(), match.getMatchDate(),
                    match.getCourtId(), MatchStatus.occupiesCourt(match.getStatus()), endTimes.get(match.getId()));
        }
        blockCourtsHeldByOtherEvents(planner, event.getId(), courtIds, rules.startDate());
        return planner;
    }

    /**
     * The courts the matches are spread over: all the active courts of the complexes the event is played in. An event
     * played in no complex keeps the old behavior: the single court of its config, or none (a list with one null).
     */
    private List<UUID> resolveCourtIds(UUID eventId, EventScheduleConfigEntity config) {
        List<UUID> sportComplexIds = eventsSportComplexesRepository.findAllActiveByEventId(eventId).stream()
                .map(EventsSportComplexesEntity::getSportComplexId)
                .toList();
        if (sportComplexIds.isEmpty()) {
            return Collections.singletonList(config.getCourtId());
        }

        List<UUID> courtIds = courtRepository.findAllActiveBySportComplexIds(sportComplexIds).stream()
                .sorted(COURT_ORDER)
                .map(CourtsEntity::getId)
                .toList();
        if (courtIds.isEmpty()) {
            throw new IllegalStateException(MatchError.EVENT_COMPLEXES_WITHOUT_COURTS.name());
        }
        return courtIds;
    }

    /** A court holds the matches of any event, not only this one's: a slot taken by another event is not free. */
    private void blockCourtsHeldByOtherEvents(CalendarPlanner planner, UUID eventId, List<UUID> courtIds, LocalDate from) {
        for (UUID courtId : courtIds) {
            if (courtId == null) {
                continue;
            }
            List<MatchesEntity> matchesOfOtherEvents = matchRepository.findAllActiveByCourtIdFrom(courtId, from.atStartOfDay())
                    .stream()
                    .filter(match -> !eventId.equals(match.getEventId()) && MatchStatus.occupiesCourt(match.getStatus()))
                    .toList();
            Map<UUID, LocalDateTime> endTimes = matchTimingService.getEndTimes(matchesOfOtherEvents);
            for (MatchesEntity match : matchesOfOtherEvents) {
                planner.blockCourt(courtId, match.getMatchDate(), endTimes.get(match.getId()));
            }
        }
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

    private List<MatchResponse> scheduleMatches(UUID eventId, List<UUID[]> pairings, ScheduleRules rules, CalendarPlanner planner) {
        List<MatchResponse> created = new ArrayList<>();

        for (UUID[] pairing : pairings) {
            created.add(createScheduledMatch(eventId, pairing, rules, planner, match -> { }));
        }
        return created;
    }

    /**
     * Creates the matches a championship phase needs, each on the first free slot of the event's calendar (the same
     * rules as {@link #generateMatches}: play days, no holidays, nobody plays twice in a day, courts spread over the
     * event's complexes). The matches already in the event keep their slots; nothing is replaced.
     */
    @Override
    public List<MatchResponse> scheduleDrafts(UUID eventId, List<MatchDraft> drafts) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        EventScheduleConfigEntity config = configRepository.findActiveByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(ChampionshipError.SCHEDULE_CONFIG_REQUIRED.name()));

        ScheduleRules rules = buildRules(event, config, firstSchedulableDate(event));
        // A round of an elimination may need extra time, so its matches reserve their court for that too.
        if (isEliminationRound(drafts)) {
            rules = rules.withExtraSlotMinutes(settingsReader.forEvent(eventId).extraTimeMinutes());
        }
        CalendarPlanner planner = planCalendar(event, config, rules, matchRepository.findAllActiveByEventId(eventId));
        List<MatchResponse> created = new ArrayList<>();

        for (MatchDraft draft : drafts) {
            UUID[] pairing = {draft.homeTeamId(), draft.awayTeamId()};
            created.add(createScheduledMatch(eventId, pairing, rules, planner, match -> tagWithChampionshipPlace(match, draft)));
        }
        return created;
    }

    /** True when every match to create belongs to an elimination (a stage of a phase that must produce winners). */
    private boolean isEliminationRound(List<MatchDraft> drafts) {
        return !drafts.isEmpty() && drafts.stream()
                .allMatch(draft -> draft.stage() != null && draft.stage().phase().isElimination());
    }

    private MatchResponse createScheduledMatch(UUID eventId, UUID[] pairing, ScheduleRules rules, CalendarPlanner planner,
                                               Consumer<MatchesEntity> tagger) {
        CalendarPlanner.Slot slot = planner.findFreeSlot(pairing[0], pairing[1])
                .orElseThrow(() -> new IllegalStateException(
                    "Could not schedule all matches within " + MAX_SEARCH_YEARS +
                    " years from " + rules.startDate() + ". Check your play days configuration."));

        MatchesEntity match = newScheduledMatch(eventId, pairing, slot);
        tagger.accept(match);
        MatchResponse response = matchMapper.toResponse(matchRepository.save(match));
        response.setEndsAt(slot.kickOff().plusMinutes(rules.slotDurationMinutes()));
        planner.book(slot, pairing[0], pairing[1]);
        return response;
    }

    private void tagWithChampionshipPlace(MatchesEntity match, MatchDraft draft) {
        match.setStage(draft.stage());
        match.setRoundNumber(draft.roundNumber());
        match.setGroupNumber(draft.groupNumber());
        match.setTieNumber(draft.tieNumber());
        match.setLegNumber(draft.legNumber());
    }

    private MatchesEntity newScheduledMatch(UUID eventId, UUID[] pairing, CalendarPlanner.Slot slot) {
        MatchesEntity match = new MatchesEntity();
        match.setEventId(eventId);
        match.setHomeTeamId(pairing[0]);
        match.setAwayTeamId(pairing[1]);
        match.setMatchDate(slot.kickOff());
        match.setStatus(MatchStatus.SCHEDULED.name());
        if (slot.courtId() != null) {
            match.setCourtId(slot.courtId());
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
}
