package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.application.iservices.ITeamStatsService;
import com.platform.backend.modules.matches.application.services.TeamStatsCalculator.EventTotals;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.ITeamStatsRepository;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamStatsResponse.TeamStatsResponse;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What each team has done: its totals, its matches and how its players did with it. The numbers are added up by the
 * database ({@link ITeamStatsRepository}); a match counts once it has a result, as in the standings of an event.
 */
@Service
@RequiredArgsConstructor
public class TeamStatsService implements ITeamStatsService {

    private final ITeamRepository teamRepository;
    private final ITeamStatsRepository teamStatsRepository;
    private final IEventRepository eventRepository;
    private final IPlayerRepository playerRepository;
    private final ITeamMemberRepository teamMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public TeamStatsResponse getTeamStats(UUID teamId) {
        return statsOf(List.of(findVisibleTeam(teamId))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamStatsResponse> getAllTeamStats() {
        List<TeamsEntity> teams = teamRepository.findAllActive().stream()
                .sorted(Comparator.comparing(TeamsEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return statsOf(teams);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TeamMatchHistoryResponse> getTeamMatches(UUID teamId, PageQuery pageQuery) {
        findVisibleTeam(teamId);
        PageResult<MatchesEntity> page = teamStatsRepository.findMatchesOfTeamNewestFirst(teamId, pageQuery);
        Map<UUID, String> eventNames = namesOfEvents(page.content());
        Map<UUID, String> teamNames = namesOfTeams(page.content());
        return page.map(match -> TeamStatsCalculator.historyLine(match, teamId, eventNames, teamNames));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamPlayerStatsResponse> getTeamPlayerStats(UUID teamId) {
        findVisibleTeam(teamId);
        List<UUID> rosterPlayerIds = teamMemberRepository.findAllActiveByTeamId(teamId).stream()
                .map(TeamMembersEntity::getPlayer)
                .map(player -> player.getId())
                .toList();
        Map<UUID, EventTotals> totalsByPlayer =
                TeamStatsCalculator.sumEventsByGroup(teamStatsRepository.countMatchEventsByPlayerAndType(teamId));
        Map<UUID, Long> matchesWithEvents = teamStatsRepository.countMatchesWithEventsByPlayer(teamId).stream()
                .collect(Collectors.toMap(GroupedCount::groupId, GroupedCount::count));
        long teamPlayedMatches = totalsOf(List.of(teamId)).getOrDefault(teamId, TeamMatchTally.empty(teamId)).played();

        Set<UUID> everyPlayerId = new HashSet<>(rosterPlayerIds);
        everyPlayerId.addAll(totalsByPlayer.keySet());
        Map<UUID, String> playerNames = everyPlayerId.isEmpty() ? Map.of() : playerRepository.findFullNamesByIds(everyPlayerId);

        List<TeamPlayerStatsResponse> ranking = new ArrayList<>();
        for (UUID playerId : everyPlayerId) {
            boolean onRoster = rosterPlayerIds.contains(playerId);
            long withEvents = matchesWithEvents.getOrDefault(playerId, 0L);
            ranking.add(toPlayerLine(playerId, playerNames.get(playerId), onRoster,
                    onRoster ? Math.max(teamPlayedMatches, withEvents) : withEvents,
                    totalsByPlayer.getOrDefault(playerId, EventTotals.NONE)));
        }
        ranking.sort(TeamStatsCalculator.PLAYER_RANKING_ORDER);
        return ranking;
    }

    private TeamsEntity findVisibleTeam(UUID teamId) {
        return teamRepository.findActiveById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
    }

    private List<TeamStatsResponse> statsOf(List<TeamsEntity> teams) {
        List<UUID> teamIds = teams.stream().map(TeamsEntity::getId).toList();
        Map<UUID, TeamMatchTally> matchTotals = totalsOf(teamIds);
        Map<UUID, EventTotals> eventTotals =
                TeamStatsCalculator.sumEventsByGroup(teamStatsRepository.countMatchEventsByTeamAndType(teamIds));
        Map<UUID, Long> enrollments = teamStatsRepository.countEnrollmentsByTeam(teamIds).stream()
                .collect(Collectors.toMap(GroupedCount::groupId, GroupedCount::count));
        return teams.stream()
                .map(team -> toTeamLine(team,
                        matchTotals.getOrDefault(team.getId(), TeamMatchTally.empty(team.getId())),
                        eventTotals.getOrDefault(team.getId(), EventTotals.NONE),
                        enrollments.getOrDefault(team.getId(), 0L)))
                .toList();
    }

    private Map<UUID, TeamMatchTally> totalsOf(Collection<UUID> teamIds) {
        return TeamStatsCalculator.mergeSides(
                teamStatsRepository.tallyDecidedMatchesAsHome(teamIds),
                teamStatsRepository.tallyDecidedMatchesAsAway(teamIds));
    }

    private static TeamStatsResponse toTeamLine(TeamsEntity team, TeamMatchTally matches, EventTotals events, long enrollments) {
        TeamStatsResponse line = new TeamStatsResponse();
        line.setTeamId(team.getId());
        line.setTeamName(team.getName());
        line.setOrganizationId(team.getOrganizationId());
        line.setEventsEnrolled((int) enrollments);
        line.setMatchesPlayed((int) matches.played());
        line.setWins((int) matches.wins());
        line.setDraws((int) matches.draws());
        line.setLosses((int) matches.losses());
        line.setGoalsFor((int) matches.goalsFor());
        line.setGoalsAgainst((int) matches.goalsAgainst());
        line.setGoalDifference((int) (matches.goalsFor() - matches.goalsAgainst()));
        line.setYellowCards(events.yellowCards());
        line.setRedCards(events.redCards());
        line.setFouls(events.fouls());
        return line;
    }

    private static TeamPlayerStatsResponse toPlayerLine(UUID playerId, String name, boolean onRoster,
                                                        long matchesPlayed, EventTotals totals) {
        TeamPlayerStatsResponse line = new TeamPlayerStatsResponse();
        line.setPlayerId(playerId);
        line.setPlayerName(name);
        line.setOnRoster(onRoster);
        line.setMatchesPlayed((int) matchesPlayed);
        line.setGoals(totals.goals());
        line.setAssists(totals.assists());
        line.setYellowCards(totals.yellowCards());
        line.setRedCards(totals.redCards());
        line.setFouls(totals.fouls());
        return line;
    }

    /** Only the events of the matches on the page are looked up (the ones the caller can read have a name). */
    private Map<UUID, String> namesOfEvents(List<MatchesEntity> matches) {
        List<UUID> eventIds = matches.stream().map(MatchesEntity::getEventId).filter(id -> id != null).distinct().toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findAllActiveByIds(eventIds).stream()
                .collect(Collectors.toMap(EventsEntity::getId, EventsEntity::getName));
    }

    private Map<UUID, String> namesOfTeams(List<MatchesEntity> matches) {
        List<UUID> teamIds = matches.stream()
                .flatMap(match -> Stream.of(match.getHomeTeamId(), match.getAwayTeamId()))
                .distinct()
                .toList();
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return teamRepository.findAllActiveByIds(teamIds).stream()
                .collect(Collectors.toMap(TeamsEntity::getId, TeamsEntity::getName));
    }
}
