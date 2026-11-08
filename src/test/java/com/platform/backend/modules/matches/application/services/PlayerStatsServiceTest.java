package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchEventType;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.PlayerTotalsResponse.PlayerTotalsResponse;
import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock private IEventRepository eventRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IMatchEventRepository matchEventRepository;
    @Mock private IPlayerRepository playerRepository;
    @Mock private ITeamRepository teamRepository;

    @InjectMocks
    private PlayerStatsService playerStatsService;

    private final UUID eventId = UUID.randomUUID();
    private final MatchesEntity match = matchWithId();
    private TeamsEntity tigres;
    private TeamsEntity leones;
    private PlayersEntity ana;
    private PlayersEntity luis;
    private PlayersEntity eva;

    @BeforeEach
    void setUp() {
        tigres = team("Tigres");
        leones = team("Leones");
        ana = player("Ana", "Lopez");
        luis = player("Luis", "Mora");
        eva = player("Eva", "Rios");

        lenient().when(eventRepository.findActiveById(eventId)).thenReturn(Optional.of(new EventsEntity()));
        lenient().when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(match));
        lenient().when(playerRepository.findFullNamesByIds(any())).thenReturn(Map.of(
                ana.getId(), "Ana Lopez", luis.getId(), "Luis Mora", eva.getId(), "Eva Rios"));
        lenient().when(teamRepository.findAllActiveByIds(any())).thenReturn(List.of(tigres, leones));
    }

    private static MatchesEntity matchWithId() {
        MatchesEntity match = new MatchesEntity();
        match.setId(UUID.randomUUID());
        return match;
    }

    private static TeamsEntity team(String name) {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName(name);
        return team;
    }

    private static PlayersEntity player(String firstName, String lastName) {
        PlayersEntity player = new PlayersEntity();
        player.setId(UUID.randomUUID());
        player.setFirstName(firstName);
        player.setLastName(lastName);
        return player;
    }

    private MatchEventsEntity event(PlayersEntity player, TeamsEntity team, MatchEventType type) {
        MatchEventsEntity event = new MatchEventsEntity();
        event.setMatchId(match.getId());
        event.setPlayerId(player.getId());
        event.setTeamId(team.getId());
        event.setEventType(type.name());
        return event;
    }

    private void givenTimeline(MatchEventsEntity... events) {
        when(matchEventRepository.findAllActiveByMatchIds(List.of(match.getId()))).thenReturn(List.of(events));
    }

    @Test
    void getPlayerStatsByEventId_whenPlayersScoredAndWereCarded_thenEachLineAddsUpItsOwnEvents() {
        givenTimeline(
                event(ana, tigres, MatchEventType.GOAL), event(ana, tigres, MatchEventType.GOAL),
                event(ana, tigres, MatchEventType.YELLOW_CARD), event(luis, leones, MatchEventType.ASSIST),
                event(luis, leones, MatchEventType.RED_CARD), event(luis, leones, MatchEventType.FOUL));

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        PlayerStatsResponse anaLine = stats.get(0);
        assertThat(anaLine.getPlayerName()).isEqualTo("Ana Lopez");
        assertThat(anaLine.getTeamName()).isEqualTo("Tigres");
        assertThat(anaLine.getGoals()).isEqualTo(2);
        assertThat(anaLine.getYellowCards()).isEqualTo(1);
        PlayerStatsResponse luisLine = stats.get(1);
        assertThat(luisLine.getAssists()).isEqualTo(1);
        assertThat(luisLine.getRedCards()).isEqualTo(1);
        assertThat(luisLine.getFouls()).isEqualTo(1);
        assertThat(luisLine.getGoals()).isZero();
    }

    @Test
    void getPlayerStatsByEventId_whenSeveralPlayers_thenMostGoalsFirstThenAssistsThenFewerCardsThenName() {
        givenTimeline(
                event(eva, tigres, MatchEventType.GOAL), event(luis, leones, MatchEventType.GOAL),
                event(ana, tigres, MatchEventType.GOAL), event(ana, tigres, MatchEventType.GOAL),
                event(luis, leones, MatchEventType.ASSIST), event(eva, tigres, MatchEventType.YELLOW_CARD));

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        // Ana 2 goals; Luis and Eva 1 goal each, but Luis also assisted.
        assertThat(stats).extracting(PlayerStatsResponse::getPlayerName).containsExactly("Ana Lopez", "Luis Mora", "Eva Rios");
    }

    @Test
    void getPlayerStatsByEventId_whenAnEventTypeIsNotKnown_thenItIsIgnoredButThePlayerStillAppears() {
        MatchEventsEntity unknown = event(ana, tigres, MatchEventType.GOAL);
        unknown.setEventType("SOMETHING_ELSE");
        givenTimeline(unknown);

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        assertThat(stats).singleElement().satisfies(line -> assertThat(line.getGoals()).isZero());
    }

    @Test
    void getPlayerStatsByEventId_whenTheEventHasNoMatches_thenThereIsNothingToCount() {
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of());

        assertThat(playerStatsService.getPlayerStatsByEventId(eventId)).isEmpty();
        verifyNoInteractions(matchEventRepository);
    }

    @Test
    void getPlayerStatsByEventId_whenTheEventDoesNotExist_thenNotFound() {
        assertThatThrownBy(() -> playerStatsService.getPlayerStatsByEventId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    // ─── matches played in the event ────────────────────────────────────────────

    private MatchesEntity playedMatch(TeamsEntity home, TeamsEntity away, Integer homeScore, Integer awayScore) {
        MatchesEntity played = new MatchesEntity();
        played.setId(UUID.randomUUID());
        played.setHomeTeamId(home.getId());
        played.setAwayTeamId(away.getId());
        played.setHomeScore(homeScore);
        played.setAwayScore(awayScore);
        return played;
    }

    @Test
    void getPlayerStatsByEventId_whenTheTeamPlayedSeveralMatches_thenEachLineCountsOnlyThoseWithAResultOfItsTeam() {
        TeamsEntity pumas = team("Pumas");
        MatchesEntity tigresWon = playedMatch(tigres, leones, 2, 0);
        MatchesEntity tigresDrew = playedMatch(pumas, tigres, 1, 1);
        MatchesEntity tigresAhead = playedMatch(tigres, pumas, null, null);
        MatchesEntity leonesOnly = playedMatch(leones, pumas, 3, 3);
        when(matchRepository.findAllActiveByEventId(eventId)).thenReturn(List.of(tigresWon, tigresDrew, tigresAhead, leonesOnly));
        when(matchEventRepository.findAllActiveByMatchIds(any())).thenReturn(List.of(
                event(ana, tigres, MatchEventType.GOAL), event(luis, leones, MatchEventType.GOAL)));

        List<PlayerStatsResponse> stats = playerStatsService.getPlayerStatsByEventId(eventId);

        assertThat(stats).extracting(PlayerStatsResponse::getPlayerName, PlayerStatsResponse::getMatchesPlayed)
                .containsExactlyInAnyOrder(tuple("Ana Lopez", 2), tuple("Luis Mora", 2));
    }

    @Test
    void getPlayerStatsByEventId_whenNoMatchHasAResultYet_thenMatchesPlayedIsZero() {
        givenTimeline(event(ana, tigres, MatchEventType.GOAL));

        assertThat(playerStatsService.getPlayerStatsByEventId(eventId)).singleElement()
                .satisfies(line -> assertThat(line.getMatchesPlayed()).isZero());
    }

    // ─── totals of a player across every event ──────────────────────────────────

    private MatchEventsEntity eventIn(MatchesEntity inMatch, PlayersEntity player, TeamsEntity team, MatchEventType type) {
        MatchEventsEntity event = new MatchEventsEntity();
        event.setMatchId(inMatch.getId());
        event.setPlayerId(player.getId());
        event.setTeamId(team.getId());
        event.setEventType(type.name());
        return event;
    }

    @Test
    void getPlayerTotals_whenThePlayerHasEventsInSeveralMatches_thenEverythingIsAddedUp() {
        ana.setTeam(tigres);
        MatchesEntity first = playedMatch(tigres, leones, 2, 1);
        MatchesEntity second = playedMatch(leones, tigres, 0, 0);
        when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId())).thenReturn(List.of(
                eventIn(first, ana, tigres, MatchEventType.GOAL), eventIn(first, ana, tigres, MatchEventType.GOAL),
                eventIn(first, ana, tigres, MatchEventType.ASSIST), eventIn(second, ana, tigres, MatchEventType.YELLOW_CARD),
                eventIn(second, ana, tigres, MatchEventType.RED_CARD), eventIn(second, ana, tigres, MatchEventType.FOUL)));
        when(matchRepository.findAllActiveByTeamId(tigres.getId())).thenReturn(List.of(first, second));

        PlayerTotalsResponse totals = playerStatsService.getPlayerTotals(ana.getId());

        assertThat(totals.getPlayerId()).isEqualTo(ana.getId());
        assertThat(totals.getGoals()).isEqualTo(2);
        assertThat(totals.getAssists()).isEqualTo(1);
        assertThat(totals.getYellowCards()).isEqualTo(1);
        assertThat(totals.getRedCards()).isEqualTo(1);
        assertThat(totals.getFouls()).isEqualTo(1);
        assertThat(totals.getMatchesPlayed()).isEqualTo(2);
    }

    @Test
    void getPlayerTotals_whenTheTeamPlayedMatchesWithoutTheirEvents_thenThosePlayedCountAsMatchesPlayed() {
        ana.setTeam(tigres);
        MatchesEntity playedWithoutAnaDoingAnything = playedMatch(tigres, leones, 1, 0);
        MatchesEntity notPlayedYet = playedMatch(tigres, leones, null, null);
        when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId())).thenReturn(List.of());
        when(matchRepository.findAllActiveByTeamId(tigres.getId()))
                .thenReturn(List.of(playedWithoutAnaDoingAnything, notPlayedYet));

        assertThat(playerStatsService.getPlayerTotals(ana.getId()).getMatchesPlayed()).isEqualTo(1);
    }

    @Test
    void getPlayerTotals_whenAMatchHasTheirEventAndIsOfTheirTeamToo_thenItIsCountedOnce() {
        ana.setTeam(tigres);
        MatchesEntity match = playedMatch(tigres, leones, 1, 0);
        when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId())).thenReturn(List.of(
                eventIn(match, ana, tigres, MatchEventType.GOAL), eventIn(match, ana, tigres, MatchEventType.ASSIST)));
        when(matchRepository.findAllActiveByTeamId(tigres.getId())).thenReturn(List.of(match));

        assertThat(playerStatsService.getPlayerTotals(ana.getId()).getMatchesPlayed()).isEqualTo(1);
    }

    @Test
    void getPlayerTotals_whenThePlayerScoredForATeamTheyLeft_thenThoseEventsAndMatchesStillCount() {
        // Ana has no team now, but she scored for Leones in an earlier match.
        ana.setTeam(null);
        MatchesEntity oldMatch = playedMatch(leones, tigres, 1, 0);
        when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId()))
                .thenReturn(List.of(eventIn(oldMatch, ana, leones, MatchEventType.GOAL)));

        PlayerTotalsResponse totals = playerStatsService.getPlayerTotals(ana.getId());

        assertThat(totals.getGoals()).isEqualTo(1);
        assertThat(totals.getMatchesPlayed()).isEqualTo(1);
        verify(matchRepository, never()).findAllActiveByTeamId(any());
    }

    @Test
    void getPlayerTotals_whenThePlayerHasNothingYet_thenEverythingIsZero() {
        when(playerRepository.findActiveById(ana.getId())).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId())).thenReturn(List.of());

        PlayerTotalsResponse totals = playerStatsService.getPlayerTotals(ana.getId());

        assertThat(totals.getGoals() + totals.getAssists() + totals.getYellowCards() + totals.getRedCards()
                + totals.getFouls() + totals.getMatchesPlayed()).isZero();
    }

    @Test
    void getPlayerTotals_whenThePlayerIsNotVisibleToTheCaller_thenNotFoundAndNothingIsCounted() {
        UUID invisible = UUID.randomUUID();
        when(playerRepository.findActiveById(invisible)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerStatsService.getPlayerTotals(invisible)).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(matchEventRepository);
    }

    @Test
    void getCurrentPlayerTotals_whenTheAccountHasAPlayer_thenItsTotalsAreReturned() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "Ana@Email.com");
        when(playerRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));
        when(matchEventRepository.findAllActiveByPlayerId(ana.getId()))
                .thenReturn(List.of(event(ana, tigres, MatchEventType.GOAL)));

        PlayerTotalsResponse totals = playerStatsService.getCurrentPlayerTotals();

        assertThat(totals.getPlayerId()).isEqualTo(ana.getId());
        assertThat(totals.getGoals()).isEqualTo(1);
    }

    @Test
    void getCurrentPlayerTotals_whenTheAccountHasNoPlayerOrItWasDeleted_thenNotFound() {
        TenantTestSupport.actAsMobileAccount(UUID.randomUUID(), "ana@email.com");
        when(playerRepository.findByEmail("ana@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerStatsService.getCurrentPlayerTotals()).isInstanceOf(EntityNotFoundException.class);

        ana.setDeletedAt(LocalDateTime.now());
        when(playerRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> playerStatsService.getCurrentPlayerTotals()).isInstanceOf(EntityNotFoundException.class);
    }
}
