package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.TeamMatchResult;
import com.platform.backend.modules.matches.domain.irepositories.ITeamStatsRepository;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamStatsResponse.TeamStatsResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamStatsServiceTest {

    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamStatsRepository teamStatsRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TeamStatsService teamStatsService;

    private TeamsEntity tigres;
    private TeamsEntity leones;
    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tigres = team("Tigres");
        leones = team("leones");
        lenient().when(teamStatsRepository.tallyDecidedMatchesAsHome(any())).thenReturn(List.of());
        lenient().when(teamStatsRepository.tallyDecidedMatchesAsAway(any())).thenReturn(List.of());
        lenient().when(teamStatsRepository.countEnrollmentsByTeam(any())).thenReturn(List.of());
        lenient().when(teamStatsRepository.countMatchEventsByTeamAndType(any())).thenReturn(List.of());
        lenient().when(teamStatsRepository.countMatchEventsByPlayerAndType(any())).thenReturn(List.of());
        lenient().when(teamStatsRepository.countMatchesWithEventsByPlayer(any())).thenReturn(List.of());
        lenient().when(teamRepository.findActiveById(tigres.getId())).thenReturn(Optional.of(tigres));
        lenient().when(teamRepository.findActiveById(leones.getId())).thenReturn(Optional.of(leones));
    }

    private TeamsEntity team(String name) {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName(name);
        team.setOrganizationId(organizationId);
        return team;
    }

    private static PlayersEntity player(String firstName) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setFirstName(firstName);
        return player;
    }

    private static TeamMembersEntity member(PlayersEntity player, TeamsEntity team) {
        return TeamMembersEntity.of(player, team);
    }

    // ─── totals of a team ───────────────────────────────────────────────────────

    @Test
    void getTeamStats_addsBothSidesTheEnrollmentsAndTheCards() {
        UUID teamId = tigres.getId();
        when(teamStatsRepository.tallyDecidedMatchesAsHome(List.of(teamId)))
                .thenReturn(List.of(new TeamMatchTally(teamId, 2, 1, 1, 0, 5, 3)));
        when(teamStatsRepository.tallyDecidedMatchesAsAway(List.of(teamId)))
                .thenReturn(List.of(new TeamMatchTally(teamId, 1, 0, 0, 1, 0, 2)));
        when(teamStatsRepository.countEnrollmentsByTeam(List.of(teamId))).thenReturn(List.of(new GroupedCount(teamId, null, 2)));
        when(teamStatsRepository.countMatchEventsByTeamAndType(List.of(teamId))).thenReturn(List.of(
                new GroupedCount(teamId, "YELLOW_CARD", 4), new GroupedCount(teamId, "RED_CARD", 1),
                new GroupedCount(teamId, "FOUL", 9), new GroupedCount(teamId, "GOAL", 5)));

        TeamStatsResponse stats = teamStatsService.getTeamStats(teamId);

        assertThat(stats.getTeamName()).isEqualTo("Tigres");
        assertThat(stats.getOrganizationId()).isEqualTo(organizationId);
        assertThat(stats.getEventsEnrolled()).isEqualTo(2);
        assertThat(stats.getMatchesPlayed()).isEqualTo(3);
        assertThat(stats.getWins()).isEqualTo(1);
        assertThat(stats.getDraws()).isEqualTo(1);
        assertThat(stats.getLosses()).isEqualTo(1);
        assertThat(stats.getGoalsFor()).isEqualTo(5);
        assertThat(stats.getGoalsAgainst()).isEqualTo(5);
        assertThat(stats.getGoalDifference()).isZero();
        assertThat(stats.getYellowCards()).isEqualTo(4);
        assertThat(stats.getRedCards()).isEqualTo(1);
        assertThat(stats.getFouls()).isEqualTo(9);
    }

    @Test
    void getTeamStats_aTeamWithNothingPlayedHasZeros() {
        TeamStatsResponse stats = teamStatsService.getTeamStats(tigres.getId());

        assertThat(stats.getMatchesPlayed()).isZero();
        assertThat(stats.getGoalsFor()).isZero();
        assertThat(stats.getEventsEnrolled()).isZero();
        assertThat(stats.getYellowCards()).isZero();
    }

    @Test
    void getTeamStats_aTeamTheCallerCannotSeeIsNotFound() {
        UUID other = UUID.randomUUID();
        when(teamRepository.findActiveById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamStatsService.getTeamStats(other)).isInstanceOf(EntityNotFoundException.class);
        verify(teamStatsRepository, never()).tallyDecidedMatchesAsHome(any());
    }

    // ─── totals of every team ───────────────────────────────────────────────────

    @Test
    void getAllTeamStats_listsEveryVisibleTeamByNameEvenWithoutData() {
        when(teamRepository.findAllActive()).thenReturn(List.of(tigres, leones));
        when(teamStatsRepository.tallyDecidedMatchesAsHome(any()))
                .thenReturn(List.of(new TeamMatchTally(tigres.getId(), 1, 1, 0, 0, 2, 0)));

        List<TeamStatsResponse> all = teamStatsService.getAllTeamStats();

        assertThat(all).extracting(TeamStatsResponse::getTeamName).containsExactly("leones", "Tigres");
        assertThat(all.get(0).getMatchesPlayed()).isZero();
        assertThat(all.get(1).getWins()).isEqualTo(1);
        // One read of each kind for the whole list, not one per team.
        verify(teamStatsRepository).tallyDecidedMatchesAsHome(any());
        verify(teamStatsRepository).countMatchEventsByTeamAndType(any());
    }

    @Test
    void getAllTeamStats_withoutTeamsIsAnEmptyReport() {
        when(teamRepository.findAllActive()).thenReturn(List.of());

        assertThat(teamStatsService.getAllTeamStats()).isEmpty();
    }

    // ─── history ────────────────────────────────────────────────────────────────

    @Test
    void getTeamMatches_tellsEachMatchFromTheTeamsSideWithTheNamesOfTheEventAndTheOpponent() {
        UUID eventId = UUID.randomUUID();
        MatchesEntity played = new MatchesEntity();
        played.setId(UUID.randomUUID());
        played.setHomeTeamId(leones.getId());
        played.setAwayTeamId(tigres.getId());
        played.setHomeScore(0);
        played.setAwayScore(2);
        played.setEventId(eventId);
        played.setMatchDate(LocalDateTime.of(2026, 11, 3, 18, 0));
        PageQuery pageQuery = new PageQuery(0, 10);
        when(teamStatsRepository.findMatchesOfTeamNewestFirst(tigres.getId(), pageQuery))
                .thenReturn(new PageResult<>(List.of(played), 11, pageQuery));
        EventsEntity event = new EventsEntity();
        event.setId(eventId);
        event.setName("Liga Norte");
        when(eventRepository.findAllActiveByIds(List.of(eventId))).thenReturn(List.of(event));
        when(teamRepository.findAllActiveByIds(any())).thenReturn(List.of(tigres, leones));

        PageResult<TeamMatchHistoryResponse> page = teamStatsService.getTeamMatches(tigres.getId(), pageQuery);

        assertThat(page.totalElements()).isEqualTo(11);
        TeamMatchHistoryResponse line = page.content().get(0);
        assertThat(line.getEventName()).isEqualTo("Liga Norte");
        assertThat(line.getOpponentName()).isEqualTo("leones");
        assertThat(line.getGoalsFor()).isEqualTo(2);
        assertThat(line.getResult()).isEqualTo(TeamMatchResult.WIN);
    }

    @Test
    void getTeamMatches_aTeamTheCallerCannotSeeIsNotFound() {
        UUID other = UUID.randomUUID();
        when(teamRepository.findActiveById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamStatsService.getTeamMatches(other, new PageQuery(0, 10)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getTeamMatches_anEmptyHistoryReadsNoNames() {
        PageQuery pageQuery = new PageQuery(0, 10);
        when(teamStatsRepository.findMatchesOfTeamNewestFirst(tigres.getId(), pageQuery)).thenReturn(PageResult.empty(pageQuery));

        assertThat(teamStatsService.getTeamMatches(tigres.getId(), pageQuery).content()).isEmpty();
        verify(eventRepository, never()).findAllActiveByIds(any());
        verify(teamRepository, never()).findAllActiveByIds(any());
    }

    // ─── ranking of the players of a team ───────────────────────────────────────

    @Test
    void getTeamPlayerStats_theRosterIsRankedWithZerosAndAFormerPlayerWithEventsStaysIn() {
        PlayersEntity ana = player("Ana");
        PlayersEntity luis = player("Luis");
        PlayersEntity eva = player("Eva");
        PlayersEntity beto = player("Beto");
        UUID teamId = tigres.getId();
        when(teamMemberRepository.findAllActiveByTeamId(teamId))
                .thenReturn(List.of(member(ana, tigres), member(luis, tigres), member(eva, tigres)));
        when(teamStatsRepository.countMatchEventsByPlayerAndType(teamId)).thenReturn(List.of(
                new GroupedCount(ana.getId(), "GOAL", 2), new GroupedCount(ana.getId(), "ASSIST", 1),
                new GroupedCount(beto.getId(), "GOAL", 3), new GroupedCount(luis.getId(), "YELLOW_CARD", 1)));
        when(teamStatsRepository.countMatchesWithEventsByPlayer(teamId)).thenReturn(List.of(
                new GroupedCount(ana.getId(), null, 2), new GroupedCount(beto.getId(), null, 1),
                new GroupedCount(luis.getId(), null, 1)));
        when(teamStatsRepository.tallyDecidedMatchesAsHome(List.of(teamId)))
                .thenReturn(List.of(new TeamMatchTally(teamId, 4, 2, 1, 1, 7, 4)));
        when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of(
                ana.getId(), "Ana Lopez", luis.getId(), "Luis Mora", eva.getId(), "Eva Rios", beto.getId(), "Beto Paz"));

        List<TeamPlayerStatsResponse> ranking = teamStatsService.getTeamPlayerStats(teamId);

        assertThat(ranking).extracting(TeamPlayerStatsResponse::getPlayerName)
                .containsExactly("Beto Paz", "Ana Lopez", "Eva Rios", "Luis Mora");
        TeamPlayerStatsResponse formerPlayer = ranking.get(0);
        assertThat(formerPlayer.isOnRoster()).isFalse();
        assertThat(formerPlayer.getGoals()).isEqualTo(3);
        // Someone who left only keeps the matches where they have an event.
        assertThat(formerPlayer.getMatchesPlayed()).isEqualTo(1);
        TeamPlayerStatsResponse withAnAssist = ranking.get(1);
        assertThat(withAnAssist.isOnRoster()).isTrue();
        assertThat(withAnAssist.getAssists()).isEqualTo(1);
        // Whoever is on the roster is credited with the matches the team played.
        assertThat(withAnAssist.getMatchesPlayed()).isEqualTo(4);
        TeamPlayerStatsResponse withoutEvents = ranking.get(2);
        assertThat(withoutEvents.getGoals()).isZero();
        assertThat(withoutEvents.getMatchesPlayed()).isEqualTo(4);
        assertThat(ranking.get(3).getYellowCards()).isEqualTo(1);
    }

    @Test
    void getTeamPlayerStats_aTeamWithoutPlayersIsAnEmptyRanking() {
        when(teamMemberRepository.findAllActiveByTeamId(tigres.getId())).thenReturn(List.of());

        assertThat(teamStatsService.getTeamPlayerStats(tigres.getId())).isEmpty();
        verify(playerRepository, never()).findFullNamesByIds(any());
    }

    @Test
    void getTeamPlayerStats_aTeamTheCallerCannotSeeIsNotFound() {
        UUID other = UUID.randomUUID();
        when(teamRepository.findActiveById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamStatsService.getTeamPlayerStats(other)).isInstanceOf(EntityNotFoundException.class);
    }
}
