package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.platform.backend.modules.matches.application.engine.EngineFixtures.match;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.teams;
import static org.assertj.core.api.Assertions.assertThat;

class TieResolverTest {

    private final List<UUID> teams = teams(2);
    private final UUID teamA = teams.get(0);
    private final UUID teamB = teams.get(1);

    @Test
    void resolve_whenASingleMatchHasAWinner_thenItAdvances() {
        MatchRef leg = leg(teamA, teamB, 2, 1, null);

        TieResolver.Outcome outcome = TieResolver.resolve(teamA, teamB, List.of(leg));

        assertThat(outcome.status()).isEqualTo(TieResolver.Status.DECIDED);
        assertThat(outcome.winnerTeamId()).isEqualTo(teamA);
        assertThat(outcome.loserTeamId()).isEqualTo(teamB);
    }

    @Test
    void resolve_whenTheTieHasNoMatchesYet_thenItIsPending() {
        assertThat(TieResolver.resolve(teamA, teamB, List.of()).status()).isEqualTo(TieResolver.Status.PENDING);
    }

    @Test
    void resolve_whenAMatchHasNoResult_thenItIsPending() {
        MatchRef notPlayed = match(teamA, teamB, null, null, MatchStage.KNOCKOUT, 1, null, 1, null);

        assertThat(TieResolver.resolve(teamA, teamB, List.of(notPlayed)).status()).isEqualTo(TieResolver.Status.PENDING);
    }

    @Test
    void resolve_whenTwoLegsAreNotBothPlayed_thenItIsPending() {
        MatchRef first = leg(teamA, teamB, 3, 0, null);
        MatchRef second = match(teamB, teamA, null, null, MatchStage.KNOCKOUT, 1, null, 1, 2);

        assertThat(TieResolver.resolve(teamA, teamB, List.of(first, second)).status()).isEqualTo(TieResolver.Status.PENDING);
    }

    @Test
    void resolve_whenTwoLegs_thenTheGoalsOfBothLegsAreAdded() {
        // A wins the first leg 1-0 and loses the return 1-3: B goes through 3-2 on aggregate.
        MatchRef first = withLeg(leg(teamA, teamB, 1, 0, null), 1);
        MatchRef second = withLeg(leg(teamB, teamA, 3, 1, null), 2);

        TieResolver.Outcome outcome = TieResolver.resolve(teamA, teamB, List.of(first, second));

        assertThat(outcome.teamAGoals()).isEqualTo(2);
        assertThat(outcome.teamBGoals()).isEqualTo(3);
        assertThat(outcome.winnerTeamId()).isEqualTo(teamB);
    }

    @Test
    void resolve_whenTheAggregateIsLevelAndNobodyWasNamed_thenItNeedsATiebreak() {
        MatchRef first = withLeg(leg(teamA, teamB, 1, 0, null), 1);
        MatchRef second = withLeg(leg(teamB, teamA, 1, 0, null), 2);

        TieResolver.Outcome outcome = TieResolver.resolve(teamA, teamB, List.of(first, second));

        assertThat(outcome.status()).isEqualTo(TieResolver.Status.NEEDS_TIEBREAK);
        assertThat(outcome.winnerTeamId()).isNull();
    }

    @Test
    void resolve_whenASingleMatchIsADrawAndATeamWasNamed_thenThatTeamAdvances() {
        MatchRef draw = leg(teamA, teamB, 1, 1, teamB);

        TieResolver.Outcome outcome = TieResolver.resolve(teamA, teamB, List.of(draw));

        assertThat(outcome.status()).isEqualTo(TieResolver.Status.DECIDED);
        assertThat(outcome.winnerTeamId()).isEqualTo(teamB);
        assertThat(outcome.loserTeamId()).isEqualTo(teamA);
    }

    @Test
    void resolve_whenTheNamedTeamIsNotInTheTie_thenItStillNeedsATiebreak() {
        MatchRef draw = leg(teamA, teamB, 1, 1, UUID.randomUUID());

        assertThat(TieResolver.resolve(teamA, teamB, List.of(draw)).status()).isEqualTo(TieResolver.Status.NEEDS_TIEBREAK);
    }

    @Test
    void resolve_whenTwoLegsAndTheAdvancingTeamIsOnTheFirstLeg_thenItIsIgnoredBecauseOnlyTheReturnLegDecides() {
        MatchRef first = withLeg(leg(teamA, teamB, 1, 0, teamA), 1);
        MatchRef second = withLeg(leg(teamB, teamA, 1, 0, null), 2);

        assertThat(TieResolver.resolve(teamA, teamB, List.of(first, second)).status()).isEqualTo(TieResolver.Status.NEEDS_TIEBREAK);
    }

    @Test
    void byeFor_whenATeamHasNoRival_thenItAdvances() {
        TieResolver.Outcome outcome = TieResolver.byeFor(teamA);

        assertThat(outcome.isDecided()).isTrue();
        assertThat(outcome.winnerTeamId()).isEqualTo(teamA);
    }

    private MatchRef leg(UUID home, UUID away, int homeGoals, int awayGoals, UUID advancing) {
        MatchRef base = match(home, away, homeGoals, awayGoals, MatchStage.KNOCKOUT, 1, null, 1, null);
        return new MatchRef(base.id(), home, away, homeGoals, awayGoals, base.stage(), 1, null, 1, null, advancing, null, "FINISHED");
    }

    private MatchRef withLeg(MatchRef match, int legNumber) {
        return new MatchRef(match.id(), match.homeTeamId(), match.awayTeamId(), match.homeScore(), match.awayScore(), match.stage(),
                match.roundNumber(), match.groupNumber(), match.tieNumber(), legNumber, match.advancingTeamId(), null, match.status());
    }
}
