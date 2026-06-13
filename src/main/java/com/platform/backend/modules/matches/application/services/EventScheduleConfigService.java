package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IEventScheduleConfigService;
import com.platform.backend.modules.matches.application.mappers.MatchMapper;
import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IEventScheduleConfigRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
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

@Service
@Transactional
@RequiredArgsConstructor
public class EventScheduleConfigService implements IEventScheduleConfigService {

    private final IEventScheduleConfigRepository configRepository;
    private final IEventRepository eventRepository;
    private final ITeamsEventsRepository teamsEventsRepository;
    private final IMatchRepository matchRepository;
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

        return toResponse(configRepository.save(config));
    }

    @Override
    public EventScheduleConfigResponse getConfig(UUID eventId) {
        return configRepository.findActiveByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Schedule config not found for event: " + eventId));
    }

    @Override
    public List<MatchResponse> generateMatches(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        EventScheduleConfigEntity config = configRepository.findActiveByEventId(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule config not found. Save a config first."));

        List<UUID> teamIds = teamsEventsRepository.findAllActiveByEventId(eventId)
                .stream()
                .map(te -> te.getTeam().getId())
                .toList();

        if (teamIds.size() < 2) {
            throw new IllegalStateException("At least 2 teams are required to generate matches.");
        }

        List<UUID[]> pairings = buildRoundRobinPairings(teamIds);
        Set<DayOfWeek> playDays = parsePlayDays(config.getPlayDays());
        int slotDuration = config.getMatchDurationMinutes() + config.getBreakBetweenMatchesMinutes();

        LocalDate startDate = event.getStartDate() != null ? event.getStartDate() : LocalDate.now();
        LocalDate endDate = event.getEndDate() != null ? event.getEndDate() : startDate.plusYears(1);

        // day -> teams already scheduled that day
        Map<LocalDate, Set<UUID>> dayTeams = new HashMap<>();
        // day -> number of match slots already assigned
        Map<LocalDate, Integer> daySlotCount = new HashMap<>();

        List<MatchResponse> created = new ArrayList<>();

        for (UUID[] pair : pairings) {
            UUID homeId = pair[0];
            UUID awayId = pair[1];
            boolean scheduled = false;

            LocalDate date = startDate;
            while (!date.isAfter(endDate)) {
                if (playDays.contains(date.getDayOfWeek())) {
                    Set<UUID> busy = dayTeams.getOrDefault(date, Collections.emptySet());
                    if (!busy.contains(homeId) && !busy.contains(awayId)) {
                        int slot = daySlotCount.getOrDefault(date, 0);
                        LocalTime matchTime = config.getStartTime().plusMinutes((long) slot * slotDuration);
                        LocalDateTime matchDateTime = LocalDateTime.of(date, matchTime);

                        MatchesEntity match = new MatchesEntity();
                        match.setEventId(eventId);
                        match.setHomeTeamId(homeId);
                        match.setAwayTeamId(awayId);
                        match.setMatchDate(matchDateTime);
                        match.setStatus("SCHEDULED");
                        if (config.getCourtId() != null) {
                            match.setCourtId(config.getCourtId());
                        }

                        created.add(matchMapper.toResponse(matchRepository.save(match)));

                        dayTeams.computeIfAbsent(date, k -> new HashSet<>()).add(homeId);
                        dayTeams.get(date).add(awayId);
                        daySlotCount.put(date, slot + 1);
                        scheduled = true;
                        break;
                    }
                }
                date = date.plusDays(1);
            }

            if (!scheduled) {
                throw new IllegalStateException(
                        "Could not schedule all matches within the event date range. " +
                        "Add more play days or extend the end date.");
            }
        }

        return created;
    }

    // ------------------------------------------------------------------ helpers

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
        for (String day : playDays.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(day.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return days;
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
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        return r;
    }
}
