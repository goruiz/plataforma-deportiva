package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock private IEventRepository eventRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private ITeamRepository teamRepository;

    @InjectMocks
    private StandingsService standingsService;

    private TeamsEntity teamA;
    private TeamsEntity teamB;
    private TeamsEntity teamC;
    private TeamsEntity teamD;

    @BeforeEach
    void setUp() {
        teamA = buildTeam("A");
        teamB = buildTeam("B");
        teamC = buildTeam("C");
        teamD = buildTeam("D");
    }

    // ─── getStandingsByEventId: orden de la tabla ───────────────────────────────

    @Test
    void getStandingsByEventId_whenPointsAreTied_thenBetterGoalDifferenceGoesFirst() {
        // A y B suman 3 puntos; B los gana por más diferencia. C y D suman 0; C pierde por menos.
        givenLeagueWithMatches(
                playedMatch(teamA, teamC, 1, 0),
                playedMatch(teamB, teamD, 3, 0));

        List<StandingEntryResponse> standings = standingsService.getStandingsByEventId(EVENT_ID);

        assertThat(teamNamesOf(standings)).containsExactly("B", "A", "C", "D");
    }

    @Test
    void getStandingsByEventId_whenPointsAndGoalDifferenceAreTied_thenMoreGoalsScoredGoesFirst() {
        // A y B ganan por 1 gol de diferencia; A anotó más. C y D pierden igual; C anotó más.
        givenLeagueWithMatches(
                playedMatch(teamA, teamC, 3, 2),
                playedMatch(teamB, teamD, 1, 0));

        List<StandingEntryResponse> standings = standingsService.getStandingsByEventId(EVENT_ID);

        assertThat(teamNamesOf(standings)).containsExactly("A", "B", "C", "D");
    }

    @Test
    void getStandingsByEventId_whenOneTeamHasMorePoints_thenItGoesFirstEvenWithWorseGoalDifference() {
        // A gana dos partidos por 1-0 y pierde 0-6 contra D: 6 pts pero DG -4. D tiene 3 pts y DG +6.
        givenLeagueWithMatches(
                playedMatch(teamA, teamC, 1, 0),
                playedMatch(teamA, teamB, 1, 0),
                playedMatch(teamD, teamA, 6, 0));

        List<StandingEntryResponse> standings = standingsService.getStandingsByEventId(EVENT_ID);

        assertThat(standings.get(0).getTeamName()).isEqualTo("A");
        assertThat(standings.get(0).getGoalDifference()).isEqualTo(-4);
        assertThat(standings.get(1).getTeamName()).isEqualTo("D");
    }

    @Test
    void getStandingsByEventId_whenTableIsBuilt_thenPositionsStartAtOneAndFollowTheOrder() {
        givenLeagueWithMatches(
                playedMatch(teamA, teamC, 1, 0),
                playedMatch(teamB, teamD, 3, 0));

        List<StandingEntryResponse> standings = standingsService.getStandingsByEventId(EVENT_ID);

        assertThat(standings).extracting(StandingEntryResponse::getPosition).containsExactly(1, 2, 3, 4);
    }

    // ─── getStandingsByEventId: partidos y tipo de evento ───────────────────────

    @Test
    void getStandingsByEventId_whenMatchHasNoScore_thenItIsNotCounted() {
        MatchesEntity unplayed = buildMatch(teamA, teamB);
        givenLeagueWithMatches(unplayed);

        List<StandingEntryResponse> standings = standingsService.getStandingsByEventId(EVENT_ID);

        assertThat(standings).isEmpty();
    }

    @Test
    void getStandingsByEventId_whenEventIsNotALeague_thenThrowIllegalArgument() {
        when(eventRepository.findActiveById(EVENT_ID))
                .thenReturn(Optional.of(buildEventOfType("torneo")));

        assertThatThrownBy(() -> standingsService.getStandingsByEventId(EVENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("league");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private void givenLeagueWithMatches(MatchesEntity... matches) {
        when(eventRepository.findActiveById(EVENT_ID)).thenReturn(Optional.of(buildEventOfType("liga")));
        when(matchRepository.findAllActiveByEventId(EVENT_ID)).thenReturn(List.of(matches));
        when(teamRepository.findAllActiveByIds(anyCollection())).thenReturn(List.of(teamA, teamB, teamC, teamD));
    }

    private EventsEntity buildEventOfType(String eventTypeName) {
        EventTypesEntity eventType = new EventTypesEntity();
        eventType.setName(eventTypeName);
        EventsEntity event = new EventsEntity();
        event.setEventType(eventType);
        return event;
    }

    private TeamsEntity buildTeam(String name) {
        TeamsEntity team = new TeamsEntity();
        team.setId(UUID.randomUUID());
        team.setName(name);
        return team;
    }

    private MatchesEntity buildMatch(TeamsEntity home, TeamsEntity away) {
        MatchesEntity match = new MatchesEntity();
        match.setHomeTeamId(home.getId());
        match.setAwayTeamId(away.getId());
        return match;
    }

    private MatchesEntity playedMatch(TeamsEntity home, TeamsEntity away, int homeGoals, int awayGoals) {
        MatchesEntity match = buildMatch(home, away);
        match.setHomeScore(homeGoals);
        match.setAwayScore(awayGoals);
        return match;
    }

    private List<String> teamNamesOf(List<StandingEntryResponse> standings) {
        return standings.stream().map(StandingEntryResponse::getTeamName).toList();
    }
}
