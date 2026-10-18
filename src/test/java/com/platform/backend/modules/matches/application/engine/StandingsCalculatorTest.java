package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.platform.backend.modules.matches.application.engine.EngineFixtures.leagueRules;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.orderOf;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.played;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.rulesWithPoints;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.rulesWithTiebreakers;
import static com.platform.backend.modules.matches.application.engine.EngineFixtures.teams;
import static org.assertj.core.api.Assertions.assertThat;

class StandingsCalculatorTest {

    @Test
    void calculate_whenTeamsHavePlayed_thenCountsPointsGoalsAndResults() {
        List<UUID> teams = teams(2);

        List<StandingRow> table = StandingsCalculator.calculate(teams, List.of(played(teams.get(0), teams.get(1), 3, 1)), leagueRules());

        StandingRow winner = table.get(0);
        assertThat(winner.teamId()).isEqualTo(teams.get(0));
        assertThat(winner.points()).isEqualTo(3);
        assertThat(winner.won()).isEqualTo(1);
        assertThat(winner.goalsFor()).isEqualTo(3);
        assertThat(winner.goalDifference()).isEqualTo(2);
        StandingRow loser = table.get(1);
        assertThat(loser.lost()).isEqualTo(1);
        assertThat(loser.points()).isZero();
        assertThat(loser.goalDifference()).isEqualTo(-2);
    }

    @Test
    void calculate_whenAMatchIsADraw_thenBothTeamsGetThePointsForADraw() {
        List<UUID> teams = teams(2);

        List<StandingRow> table = StandingsCalculator.calculate(teams, List.of(played(teams.get(0), teams.get(1), 2, 2)), leagueRules());

        assertThat(table).allSatisfy(row -> {
            assertThat(row.points()).isEqualTo(1);
            assertThat(row.drawn()).isEqualTo(1);
        });
    }

    @Test
    void calculate_whenTheFormatGivesOtherPoints_thenUsesThem() {
        List<UUID> teams = teams(3);
        List<MatchRef> matches = List.of(
                played(teams.get(0), teams.get(1), 1, 0),
                played(teams.get(1), teams.get(2), 1, 1),
                played(teams.get(2), teams.get(0), 0, 2));

        List<StandingRow> table = StandingsCalculator.calculate(teams, matches, rulesWithPoints(2, 1, 1));

        // team 0: two wins = 4; team 1: a loss and a draw = 2; team 2: a draw and a loss = 2.
        assertThat(table.get(0).points()).isEqualTo(4);
        assertThat(table.get(1).points()).isEqualTo(2);
        assertThat(table.get(2).points()).isEqualTo(2);
    }

    @Test
    void calculate_whenATeamHasNotPlayed_thenItIsInTheTableWithZeros() {
        List<UUID> teams = teams(3);

        List<StandingRow> table = StandingsCalculator.calculate(teams, List.of(played(teams.get(0), teams.get(1), 1, 0)), leagueRules());

        assertThat(table).hasSize(3);
        StandingRow idle = table.stream().filter(row -> row.teamId().equals(teams.get(2))).findFirst().orElseThrow();
        assertThat(idle.played()).isZero();
        assertThat(idle.points()).isZero();
    }

    @Test
    void calculate_whenAMatchHasNoResult_thenItDoesNotCount() {
        List<UUID> teams = teams(2);
        MatchRef notPlayed = EngineFixtures.match(teams.get(0), teams.get(1), null, null, MatchStage.LEAGUE, null, null, null, null);

        List<StandingRow> table = StandingsCalculator.calculate(teams, List.of(notPlayed), leagueRules());

        assertThat(table).allSatisfy(row -> assertThat(row.played()).isZero());
    }

    @Test
    void calculate_whenAMatchInvolvesATeamOutsideTheTable_thenItDoesNotCount() {
        List<UUID> teams = teams(3);

        List<StandingRow> table = StandingsCalculator.calculate(List.of(teams.get(0), teams.get(1)),
                List.of(played(teams.get(0), teams.get(2), 5, 0)), leagueRules());

        assertThat(table).allSatisfy(row -> assertThat(row.played()).isZero());
    }

    @Test
    void calculate_whenPointsAreTied_thenTheFirstTiebreakerDecides() {
        List<UUID> teams = teams(4);
        // 0 and 1 win 1-0 and 3-0: same points, 1 has the better goal difference.
        List<MatchRef> matches = List.of(played(teams.get(0), teams.get(2), 1, 0), played(teams.get(1), teams.get(3), 3, 0));

        List<StandingRow> table = StandingsCalculator.calculate(teams, matches, leagueRules());

        assertThat(orderOf(table).subList(0, 2)).containsExactly(teams.get(1), teams.get(0));
    }

    @Test
    void calculate_whenGoalDifferenceIsAlsoTied_thenTheNextTiebreakerDecides() {
        List<UUID> teams = teams(4);
        // 0 wins 3-2 and 1 wins 1-0: same points and goal difference, 0 scored more.
        List<MatchRef> matches = List.of(played(teams.get(0), teams.get(2), 3, 2), played(teams.get(1), teams.get(3), 1, 0));

        List<StandingRow> table = StandingsCalculator.calculate(teams, matches, leagueRules());

        assertThat(orderOf(table).subList(0, 2)).containsExactly(teams.get(0), teams.get(1));
    }

    @Test
    void calculate_whenTheOrderOfTiebreakersChanges_thenTheTableChanges() {
        List<UUID> teams = teams(4);
        // 0 wins 4-0 (goal difference +4, 4 goals) and 1 wins 5-3 (goal difference +2, 5 goals): both have 3 points.
        List<MatchRef> matches = List.of(played(teams.get(0), teams.get(2), 4, 0), played(teams.get(1), teams.get(3), 5, 3));

        List<StandingRow> goalDifferenceFirst = StandingsCalculator.calculate(teams, matches,
                rulesWithTiebreakers(TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR));
        List<StandingRow> goalsFirst = StandingsCalculator.calculate(teams, matches,
                rulesWithTiebreakers(TiebreakerCriterion.GOALS_FOR, TiebreakerCriterion.GOAL_DIFFERENCE));

        assertThat(orderOf(goalDifferenceFirst).subList(0, 2)).containsExactly(teams.get(0), teams.get(1));
        assertThat(orderOf(goalsFirst).subList(0, 2)).containsExactly(teams.get(1), teams.get(0));
    }

    @Test
    void calculate_whenTiedTeamsMetEachOther_thenHeadToHeadDecides() {
        List<UUID> teams = teams(4);
        // 0 and 1 both end with 4 points. 0 beat 1 when they met, but 1 has the better goal difference overall.
        List<MatchRef> matches = List.of(
                played(teams.get(0), teams.get(1), 1, 0),
                played(teams.get(1), teams.get(2), 5, 0),
                played(teams.get(0), teams.get(3), 0, 0),
                played(teams.get(1), teams.get(3), 1, 1),
                played(teams.get(2), teams.get(0), 1, 0));

        List<StandingRow> byGoalDifference = StandingsCalculator.calculate(teams, matches,
                rulesWithTiebreakers(TiebreakerCriterion.GOAL_DIFFERENCE));
        List<StandingRow> byHeadToHead = StandingsCalculator.calculate(teams, matches,
                rulesWithTiebreakers(TiebreakerCriterion.HEAD_TO_HEAD, TiebreakerCriterion.GOAL_DIFFERENCE));

        assertThat(byGoalDifference.get(0).teamId()).isEqualTo(teams.get(1));
        assertThat(byHeadToHead.get(0).teamId()).isEqualTo(teams.get(0));
    }

    @Test
    void calculate_whenTiedTeamsDifferInWins_thenWinsDecide() {
        List<UUID> teams = teams(5);
        UUID withOneWin = teams.get(0);
        UUID withThreeDraws = teams.get(1);
        // Both end with 3 points: one win against three draws.
        List<MatchRef> matches = List.of(
                played(withOneWin, teams.get(2), 1, 0),
                played(withThreeDraws, teams.get(2), 0, 0),
                played(withThreeDraws, teams.get(3), 0, 0),
                played(withThreeDraws, teams.get(4), 0, 0));
        List<UUID> givenWithTheDrawerFirst = List.of(withThreeDraws, withOneWin, teams.get(2), teams.get(3), teams.get(4));

        List<StandingRow> byWins = StandingsCalculator.calculate(givenWithTheDrawerFirst, matches,
                rulesWithTiebreakers(TiebreakerCriterion.WINS));
        List<StandingRow> withoutWins = StandingsCalculator.calculate(givenWithTheDrawerFirst, matches, rulesWithTiebreakers());

        assertThat(byWins.get(0).teamId()).isEqualTo(withOneWin);
        assertThat(withoutWins.get(0).teamId()).isEqualTo(withThreeDraws);
    }

    @Test
    void calculate_whenTiedTeamsFacedDifferentRivals_thenTheOpponentsPointsDecide() {
        List<UUID> teams = teams(5);
        UUID facedTheStrongerRival = teams.get(0);
        UUID facedTheWeakerRival = teams.get(1);
        // Both win once (3 points). The rival of the first has 1 point (a draw); the rival of the second has none.
        List<MatchRef> matches = List.of(
                played(facedTheStrongerRival, teams.get(2), 1, 0),
                played(facedTheWeakerRival, teams.get(3), 1, 0),
                played(teams.get(2), teams.get(4), 0, 0));
        List<UUID> givenWithTheOtherFirst = List.of(facedTheWeakerRival, facedTheStrongerRival, teams.get(2), teams.get(3), teams.get(4));

        List<StandingRow> byOpponentsPoints = StandingsCalculator.calculate(givenWithTheOtherFirst, matches,
                rulesWithTiebreakers(TiebreakerCriterion.OPPONENTS_POINTS));
        List<StandingRow> withoutIt = StandingsCalculator.calculate(givenWithTheOtherFirst, matches, rulesWithTiebreakers());

        assertThat(byOpponentsPoints.get(0).teamId()).isEqualTo(facedTheStrongerRival);
        assertThat(byOpponentsPoints.get(0).opponentsPoints()).isEqualTo(1);
        assertThat(byOpponentsPoints.get(1).teamId()).isEqualTo(facedTheWeakerRival);
        assertThat(byOpponentsPoints.get(1).opponentsPoints()).isZero();
        assertThat(withoutIt.get(0).teamId()).isEqualTo(facedTheWeakerRival);
    }

    @Test
    void calculate_whenNothingSeparatesTwoTeams_thenTheyKeepTheOrderTheyWereGivenIn() {
        List<UUID> teams = teams(3);

        List<StandingRow> table = StandingsCalculator.calculate(teams, List.of(), leagueRules());

        assertThat(orderOf(table)).containsExactlyElementsOf(teams);
        assertThat(table).extracting(StandingRow::position).containsExactly(1, 2, 3);
    }
}
