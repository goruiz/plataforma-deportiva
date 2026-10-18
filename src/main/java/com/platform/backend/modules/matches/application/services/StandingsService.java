package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.engine.ChampionshipProgress;
import com.platform.backend.modules.matches.application.engine.ChampionshipRules;
import com.platform.backend.modules.matches.application.engine.MatchRef;
import com.platform.backend.modules.matches.application.engine.StandingsCalculator;
import com.platform.backend.modules.matches.application.engine.TeamRef;
import com.platform.backend.modules.matches.application.iservices.IStandingsService;
import com.platform.backend.modules.matches.application.mappers.ChampionshipResponseMapper;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The flat points table of an event, which is what the mobile app reads. An event with a format gets the tables its
 * phases define (a group stage gives every group's table, one after another, each line with its group); an event
 * without one is a league like it always was: 3 points for a win and 1 for a draw, by goal difference and then goals.
 */
@Service
@RequiredArgsConstructor
public class StandingsService implements IStandingsService {

    private static final String LEAGUE_EVENT_TYPE_NAME = "liga";
    private static final String UNKNOWN_TEAM_NAME = "Unknown";

    private final IEventRepository eventRepository;
    private final IMatchRepository matchRepository;
    private final ITeamRepository teamRepository;
    private final ITeamsEventsRepository teamsEventsRepository;
    private final ChampionshipResponseMapper responseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<StandingEntryResponse> getStandingsByEventId(UUID eventId) {
        EventsEntity event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        List<MatchRef> matches = matchRepository.findAllActiveByEventId(eventId).stream().map(StandingsService::toMatchRef).toList();

        EventFormatsEntity format = event.getEventFormat();
        if (format != null && format.getStructure() != null) {
            return standingsOfFormat(event, ChampionshipRules.of(format), matches);
        }
        return standingsOfLegacyLeague(event, matches);
    }

    /** A format whose phases have no points table (a knockout) has no standings: the list is empty. */
    private List<StandingEntryResponse> standingsOfFormat(EventsEntity event, ChampionshipRules rules, List<MatchRef> matches) {
        List<TeamsEventsEntity> registrations = teamsEventsRepository.findAllActiveByEventId(event.getId());
        List<TeamRef> teams = registrations.stream()
                .map(registration -> new TeamRef(registration.getTeam().getId(), registration.getTeam().getName(),
                        registration.getGroupNumber(), registration.getSeed()))
                .toList();
        Map<UUID, String> teamNames = teams.stream().collect(Collectors.toMap(TeamRef::id, TeamRef::name));

        ChampionshipProgress progress = ChampionshipProgress.of(rules, teams, matches);
        return responseMapper.toStandingEntries(progress.tables(), teamNames);
    }

    private List<StandingEntryResponse> standingsOfLegacyLeague(EventsEntity event, List<MatchRef> matches) {
        if (event.getEventType() == null || !LEAGUE_EVENT_TYPE_NAME.equalsIgnoreCase(event.getEventType().getName())) {
            throw new IllegalArgumentException(
                    "Standings are only available for league-type events or events whose format has a points table.");
        }
        List<UUID> teamsThatPlayed = teamsThatPlayed(matches);
        ChampionshipProgress.TableView table = new ChampionshipProgress.TableView(ChampionshipPhase.LEAGUE, null,
                StandingsCalculator.calculate(teamsThatPlayed, matches, ChampionshipRules.legacyLeague()));
        return responseMapper.toStandingEntries(List.of(table), namesOf(teamsThatPlayed));
    }

    /** As always, only the teams that have played appear in the table of an event without a format. */
    private List<UUID> teamsThatPlayed(List<MatchRef> matches) {
        return matches.stream()
                .filter(MatchRef::isDecided)
                .flatMap(match -> Stream.of(match.homeTeamId(), match.awayTeamId()))
                .distinct()
                .toList();
    }

    private Map<UUID, String> namesOf(List<UUID> teamIds) {
        Map<UUID, String> storedNames = teamRepository.findAllActiveByIds(teamIds).stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));
        Map<UUID, String> names = new HashMap<>();
        teamIds.forEach(teamId -> names.put(teamId, storedNames.getOrDefault(teamId, UNKNOWN_TEAM_NAME)));
        return names;
    }

    private static MatchRef toMatchRef(MatchesEntity match) {
        MatchStage stage = match.getStage() == null ? MatchStage.LEAGUE : match.getStage();
        return new MatchRef(match.getId(), match.getHomeTeamId(), match.getAwayTeamId(), match.getHomeScore(), match.getAwayScore(),
                stage, match.getRoundNumber(), match.getGroupNumber(), match.getTieNumber(), match.getLegNumber(),
                match.getAdvancingTeamId(), match.getMatchDate(), match.getStatus());
    }
}
