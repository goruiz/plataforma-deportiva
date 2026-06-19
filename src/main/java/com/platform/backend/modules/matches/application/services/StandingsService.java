package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.IStandingsService;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandingsService implements IStandingsService {

    private static final String LEAGUE_EVENT_TYPE_NAME = "liga";

    private final IEventRepository eventRepository;
    private final IMatchRepository matchRepository;
    private final ITeamRepository teamRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StandingEntryResponse> getStandingsByEventId(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        String typeName = event.getEventType().getName();
        if (!LEAGUE_EVENT_TYPE_NAME.equalsIgnoreCase(typeName)) {
            throw new IllegalArgumentException(
                    "Standings are only available for league-type events. Event type: " + typeName);
        }

        List<MatchesEntity> matches = matchRepository.findAllActiveByEventId(eventId)
                .stream()
                .filter(m -> m.getHomeScore() != null && m.getAwayScore() != null)
                .toList();

        Map<UUID, StandingEntryResponse> table = new HashMap<>();

        for (MatchesEntity match : matches) {
            UUID homeId = match.getHomeTeamId();
            UUID awayId = match.getAwayTeamId();
            int homeGoals = match.getHomeScore();
            int awayGoals = match.getAwayScore();

            StandingEntryResponse home = table.computeIfAbsent(homeId, id -> buildInitialTeamStandingEntry(id));
            StandingEntryResponse away = table.computeIfAbsent(awayId, id -> buildInitialTeamStandingEntry(id));

            home.setPlayed(home.getPlayed() + 1);
            away.setPlayed(away.getPlayed() + 1);

            home.setGoalsFor(home.getGoalsFor() + homeGoals);
            home.setGoalsAgainst(home.getGoalsAgainst() + awayGoals);

            away.setGoalsFor(away.getGoalsFor() + awayGoals);
            away.setGoalsAgainst(away.getGoalsAgainst() + homeGoals);

            if (homeGoals > awayGoals) {
                home.setWon(home.getWon() + 1);
                away.setLost(away.getLost() + 1);
            } else if (awayGoals > homeGoals) {
                away.setWon(away.getWon() + 1);
                home.setLost(home.getLost() + 1);
            } else {
                home.setDrawn(home.getDrawn() + 1);
                away.setDrawn(away.getDrawn() + 1);
            }
        }

        Map<UUID, String> teamNames = teamRepository.findAllActiveByIds(table.keySet())
                .stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));

        table.forEach((id, entry) -> entry.setTeamName(teamNames.getOrDefault(id, "Unknown")));

        List<StandingEntryResponse> standings = new ArrayList<>(table.values());
        standings.forEach(entry -> {
            entry.setGoalDifference(entry.getGoalsFor() - entry.getGoalsAgainst());
            entry.setPoints(entry.getWon() * 3 + entry.getDrawn());
        });

        standings.sort(Comparator
                .comparingInt(StandingEntryResponse::getPoints).reversed()
                .thenComparingInt(StandingEntryResponse::getGoalDifference).reversed()
                .thenComparingInt(StandingEntryResponse::getGoalsFor).reversed());

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }

        return standings;
    }

    private StandingEntryResponse buildInitialTeamStandingEntry(UUID teamId) {
        StandingEntryResponse entry = new StandingEntryResponse();
        entry.setTeamId(teamId);
        return entry;
    }
}
