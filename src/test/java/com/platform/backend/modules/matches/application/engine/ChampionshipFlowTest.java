package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.enums.PhaseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.platform.backend.modules.matches.application.engine.ChampionshipSimulator.EVERYTHING_IS_A_DRAW;
import static com.platform.backend.modules.matches.application.engine.ChampionshipSimulator.STRONGER_WINS;
import static com.platform.backend.modules.matches.application.engine.ChampionshipSimulator.rules;
import static org.assertj.core.api.Assertions.assertThat;

/** Plays a whole championship of every structure and checks how many matches it takes and who wins. */
class ChampionshipFlowTest {

    // ─── Round robin ────────────────────────────────────────────────────────────

    @Test
    void roundRobin_whenSixTeamsPlayOnce_thenThereAreFifteenMatchesAndTheStrongestWins() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.ROUND_ROBIN, false, 0, 0, 0, 0, false, false, false), 6, STRONGER_WINS);

        List<ChampionshipAction> actions = championship.runToEnd();

        assertThat(actions).containsExactly(ChampionshipAction.GENERATE_CALENDAR, ChampionshipAction.WAITING_RESULTS, ChampionshipAction.FINISHED);
        assertThat(championship.matches()).hasSize(15);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
        assertThat(EngineFixtures.orderOf(championship.progress().tables().get(0).rows())).containsExactlyElementsOf(championship.teamIds());
    }

    @Test
    void roundRobin_whenFourTeamsPlayHomeAndAway_thenThereAreTwelveMatches() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.ROUND_ROBIN, true, 0, 0, 0, 0, false, false, false), 4, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matches()).hasSize(12);
        assertThat(championship.matches()).allSatisfy(match -> assertThat(match.stage()).isEqualTo(MatchStage.LEAGUE));
    }

    @Test
    void roundRobin_whenThereAreNoMatchesYet_thenTheLeagueIsReadyAndTheNextStepIsToGenerateTheCalendar() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.ROUND_ROBIN, false, 0, 0, 0, 0, false, false, false), 4, STRONGER_WINS);

        assertThat(championship.progress().phases()).extracting(ChampionshipProgress.PhaseState::status).containsExactly(PhaseStatus.READY);
        assertThat(championship.nextStep().action()).isEqualTo(ChampionshipAction.GENERATE_CALENDAR);
    }

    // ─── Swiss ──────────────────────────────────────────────────────────────────

    @Test
    void swiss_whenEightTeamsPlayThreeRounds_thenNobodyRepeatsARivalAndTheStrongestWins() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.SWISS, false, 3, 0, 0, 0, false, false, false), 8, STRONGER_WINS);

        List<ChampionshipAction> actions = championship.runToEnd();

        assertThat(actions).containsExactly(
                ChampionshipAction.GENERATE_ROUND, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.GENERATE_ROUND, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.GENERATE_ROUND, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.FINISHED);
        assertThat(championship.matches()).hasSize(12);
        Set<Set<UUID>> pairs = new HashSet<>();
        championship.matches().forEach(match -> assertThat(pairs.add(Pairing.unorderedKey(match.homeTeamId(), match.awayTeamId()))).isTrue());
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void swiss_whenEveryTeamPlaysInEveryRound_thenEachRoundHasHalfAsManyMatchesAsTeams() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.SWISS, false, 4, 0, 0, 0, false, false, false), 10, STRONGER_WINS);

        championship.runToEnd();

        for (int round = 1; round <= 4; round++) {
            int currentRound = round;
            assertThat(championship.matches().stream().filter(match -> match.roundNumber() == currentRound)).hasSize(5);
        }
    }

    @Test
    void swiss_whenTheNumberOfTeamsIsOdd_thenItCannotStart() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.SWISS, false, 3, 0, 0, 0, false, false, false), 7, STRONGER_WINS);

        ChampionshipProgress.NextStep step = championship.nextStep();

        assertThat(step.action()).isEqualTo(ChampionshipAction.NOT_ENOUGH_TEAMS);
        assertThat(step.evenTeamsRequired()).isTrue();
    }

    @Test
    void swiss_whenThereAreFewerTeamsThanRoundsNeed_thenItCannotStart() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.SWISS, false, 5, 0, 0, 0, false, false, false), 4, STRONGER_WINS);

        ChampionshipProgress.NextStep step = championship.nextStep();

        assertThat(step.action()).isEqualTo(ChampionshipAction.NOT_ENOUGH_TEAMS);
        assertThat(step.requiredTeams()).isEqualTo(6);
    }

    // ─── Knockout ───────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 12, 16})
    void knockout_whenAnyNumberOfTeamsPlaysOnce_thenOneTeamIsEliminatedPerMatchAndTheStrongestWins(int teams) {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, false, false), teams, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matches()).hasSize(teams - 1);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void knockout_whenEightTeamsAndAThirdPlaceMatch_thenTheSemiFinalLosersPlayForThirdPlace() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, true, false), 8, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matches()).hasSize(8);
        MatchRef thirdPlace = championship.matchesOfStage(MatchStage.THIRD_PLACE).get(0);
        List<UUID> teams = championship.teamIds();
        // The semi-finals are 1 v 4 and 2 v 3 (seeds): the losers are seeds 4 and 3.
        assertThat(Set.of(thirdPlace.homeTeamId(), thirdPlace.awayTeamId())).containsExactlyInAnyOrder(teams.get(3), teams.get(2));
        assertThat(championship.progress().champion()).contains(teams.get(0));
        assertThat(championship.progress().phases().get(0).status()).isEqualTo(PhaseStatus.FINISHED);
    }

    @Test
    void knockout_whenOneSemiFinalWasABye_thenThereIsNoThirdPlaceMatch() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, true, false), 3, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matchesOfStage(MatchStage.THIRD_PLACE)).isEmpty();
        assertThat(championship.matches()).hasSize(2);
    }

    @Test
    void knockout_whenSixTeams_thenTheTwoBestSeedsSkipTheFirstRound() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, false, false), 6, STRONGER_WINS);

        List<MatchRef> firstRound = championship.generate();

        List<UUID> teams = championship.teamIds();
        assertThat(firstRound).hasSize(2);
        Set<UUID> playing = new HashSet<>();
        firstRound.forEach(match -> {
            playing.add(match.homeTeamId());
            playing.add(match.awayTeamId());
        });
        assertThat(playing).doesNotContain(teams.get(0), teams.get(1));
    }

    @Test
    void knockout_whenTiesArePlayedHomeAndAway_thenEveryTieHasTwoLegsAndTheFirstLegGoesFirst() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, true, false, false), 4, STRONGER_WINS);

        List<MatchRef> semiFinals = championship.generate();

        assertThat(semiFinals).hasSize(4);
        assertThat(semiFinals.subList(0, 2)).allSatisfy(match -> assertThat(match.legNumber()).isEqualTo(1));
        assertThat(semiFinals.subList(2, 4)).allSatisfy(match -> assertThat(match.legNumber()).isEqualTo(2));
        // The better seed hosts the return leg.
        MatchRef firstLegOfTheFirstTie = semiFinals.get(0);
        assertThat(firstLegOfTheFirstTie.awayTeamId()).isEqualTo(championship.teamIds().get(0));
    }

    @Test
    void knockout_whenEveryMatchIsADraw_thenEachTieWaitsForAnAdministratorToNameTheTeamThatAdvances() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, false, false), 4, EVERYTHING_IS_A_DRAW);

        List<ChampionshipAction> actions = championship.runToEnd();

        assertThat(actions).contains(ChampionshipAction.NEEDS_TIEBREAK);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
        assertThat(championship.matches()).hasSize(3);
    }

    @Test
    void knockout_whenATieEndsLevel_thenTheNextRoundCannotBeGenerated() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, false, false), 4, EVERYTHING_IS_A_DRAW);
        championship.generate();
        championship.playPendingMatches();

        ChampionshipProgress.NextStep step = championship.nextStep();

        assertThat(step.action()).isEqualTo(ChampionshipAction.NEEDS_TIEBREAK);
        assertThat(step.matchesToResolve()).hasSize(2);
    }

    @Test
    void knockout_whenTwoLegsAreLevelOnAggregate_thenTheReturnLegNeedsTheTiebreak() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, true, false, false), 2, EVERYTHING_IS_A_DRAW);
        championship.generate();
        championship.playPendingMatches();

        ChampionshipProgress.NextStep step = championship.nextStep();

        assertThat(step.action()).isEqualTo(ChampionshipAction.NEEDS_TIEBREAK);
        MatchRef returnLeg = championship.matches().stream().filter(match -> match.legNumber() == 2).findFirst().orElseThrow();
        assertThat(step.matchesToResolve()).containsExactly(returnLeg.id());
    }

    // ─── Double elimination ─────────────────────────────────────────────────────

    @Test
    void doubleElimination_whenFourTeamsAndTheStrongestNeverLoses_thenTheGrandFinalIsTheLastMatch() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.DOUBLE_ELIMINATION, false, 0, 0, 0, 0, false, false, false), 4, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matches()).hasSize(6);
        assertThat(championship.matchesOfStage(MatchStage.WINNERS)).hasSize(3);
        assertThat(championship.matchesOfStage(MatchStage.LOSERS)).hasSize(2);
        assertThat(championship.matchesOfStage(MatchStage.GRAND_FINAL)).hasSize(1);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void doubleElimination_whenTheTeamFromTheLosersSideWinsAndTheFormatAllowsARematch_thenASecondFinalDecides() {
        ChampionshipSimulator.ResultRule weakerWinsTheFirstFinal = (match, strongestFirst) ->
                match.stage() == MatchStage.GRAND_FINAL && match.roundNumber() == 4
                        ? (strongestFirst.indexOf(match.homeTeamId()) < strongestFirst.indexOf(match.awayTeamId()) ? new int[]{0, 2} : new int[]{2, 0})
                        : STRONGER_WINS.goals(match, strongestFirst);
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.DOUBLE_ELIMINATION, false, 0, 0, 0, 0, false, false, true), 4, weakerWinsTheFirstFinal);

        championship.runToEnd();

        assertThat(championship.matchesOfStage(MatchStage.GRAND_FINAL)).hasSize(2);
        assertThat(championship.matches()).hasSize(7);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void doubleElimination_whenTheTeamFromTheLosersSideWinsAndTheFormatHasNoRematch_thenItIsTheChampion() {
        ChampionshipSimulator.ResultRule weakerWinsTheFinal = (match, strongestFirst) ->
                match.stage() == MatchStage.GRAND_FINAL
                        ? (strongestFirst.indexOf(match.homeTeamId()) < strongestFirst.indexOf(match.awayTeamId()) ? new int[]{0, 2} : new int[]{2, 0})
                        : STRONGER_WINS.goals(match, strongestFirst);
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.DOUBLE_ELIMINATION, false, 0, 0, 0, 0, false, false, false), 4, weakerWinsTheFinal);

        championship.runToEnd();

        assertThat(championship.matchesOfStage(MatchStage.GRAND_FINAL)).hasSize(1);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 11})
    void doubleElimination_whenAnyNumberOfTeamsPlays_thenNobodyLosesMoreThanTwiceAndTheStrongestWins(int teams) {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.DOUBLE_ELIMINATION, false, 0, 0, 0, 0, false, false, false), teams, STRONGER_WINS);

        championship.runToEnd();

        List<UUID> strongestFirst = championship.teamIds();
        Map<UUID, Integer> defeats = new HashMap<>();
        championship.matches().forEach(match -> {
            UUID loser = match.homeScore() < match.awayScore() ? match.homeTeamId() : match.awayTeamId();
            defeats.merge(loser, 1, Integer::sum);
        });
        assertThat(defeats.values()).allMatch(count -> count <= 2);
        assertThat(defeats).doesNotContainKey(strongestFirst.get(0));
        assertThat(championship.matches().size()).isLessThanOrEqualTo(2 * teams - 1);
        assertThat(championship.progress().champion()).contains(strongestFirst.get(0));
    }

    @Test
    void doubleElimination_whenTiesArePlayedHomeAndAway_thenEveryTieHasTwoLegs() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.DOUBLE_ELIMINATION, false, 0, 0, 0, 0, true, false, false), 4, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matches()).hasSize(12);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    // ─── Groups + knockout ──────────────────────────────────────────────────────

    @Test
    void groupsKnockout_whenEightTeamsInTwoGroups_thenTheTopTwoOfEachGroupPlaySemiFinalsAndAFinal() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.GROUPS_KNOCKOUT, false, 0, 2, 2, 0, false, false, false), 8, STRONGER_WINS);

        List<ChampionshipAction> actions = championship.runToEnd();

        assertThat(actions).containsExactly(
                ChampionshipAction.GENERATE_CALENDAR, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.GENERATE_ROUND, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.GENERATE_ROUND, ChampionshipAction.WAITING_RESULTS,
                ChampionshipAction.FINISHED);
        assertThat(championship.matchesOfStage(MatchStage.GROUP)).hasSize(12);
        assertThat(championship.matchesOfStage(MatchStage.KNOCKOUT)).hasSize(3);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void groupsKnockout_whenTheGroupsAreDrawn_thenTheyHaveTheSameNumberOfTeamsOrOneMore() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.GROUPS_KNOCKOUT, false, 0, 3, 2, 0, false, false, false), 8, STRONGER_WINS);

        championship.drawGroups();

        ChampionshipProgress progress = championship.progress();
        assertThat(progress.groupNumbers()).containsExactly(1, 2, 3);
        assertThat(progress.groupNumbers()).extracting(group -> progress.teamIdsOfGroup(group).size()).containsExactlyInAnyOrder(3, 3, 2);
    }

    @Test
    void groupsKnockout_whenGroupsPlayHomeAndAway_thenEveryGroupMatchIsPlayedTwice() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.GROUPS_KNOCKOUT, true, 0, 2, 2, 0, false, false, false), 8, STRONGER_WINS);

        championship.generate();

        assertThat(championship.matchesOfStage(MatchStage.GROUP)).hasSize(24);
    }

    @Test
    void groupsKnockout_whenThereAreFewerTeamsThanTheGroupsNeed_thenItCannotStart() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.GROUPS_KNOCKOUT, false, 0, 3, 2, 0, false, false, false), 5, STRONGER_WINS);

        ChampionshipProgress.NextStep step = championship.nextStep();

        assertThat(step.action()).isEqualTo(ChampionshipAction.NOT_ENOUGH_TEAMS);
        assertThat(step.requiredTeams()).isEqualTo(6);
    }

    @Test
    void groupsKnockout_whenTheGroupStageIsRunning_thenTheKnockoutWaitsAndEachGroupHasItsOwnTable() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.GROUPS_KNOCKOUT, false, 0, 2, 2, 0, false, false, false), 8, STRONGER_WINS);
        championship.generate();

        ChampionshipProgress progress = championship.progress();

        assertThat(progress.phases()).extracting(ChampionshipProgress.PhaseState::status)
                .containsExactly(PhaseStatus.IN_PROGRESS, PhaseStatus.PENDING);
        assertThat(progress.tables()).hasSize(2);
        assertThat(progress.tables()).allSatisfy(table -> assertThat(table.phase()).isEqualTo(ChampionshipPhase.GROUPS));
    }

    // ─── League + playoffs / Swiss + playoffs ───────────────────────────────────

    @Test
    void leagueKnockout_whenSixTeamsAndFourQualify_thenTheTopFourPlaySemiFinalsAndAFinal() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.LEAGUE_KNOCKOUT, false, 0, 0, 0, 4, false, false, false), 6, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matchesOfStage(MatchStage.LEAGUE)).hasSize(15);
        assertThat(championship.matchesOfStage(MatchStage.KNOCKOUT)).hasSize(3);
        Set<UUID> inThePlayoffs = new HashSet<>();
        championship.matchesOfStage(MatchStage.KNOCKOUT).stream().filter(match -> match.roundNumber() == 1).forEach(match -> {
            inThePlayoffs.add(match.homeTeamId());
            inThePlayoffs.add(match.awayTeamId());
        });
        assertThat(inThePlayoffs).containsExactlyInAnyOrderElementsOf(championship.teamIds().subList(0, 4));
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    @Test
    void leagueKnockout_whenTheLeagueIsStillBeingPlayed_thenThePlayoffsArePending() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.LEAGUE_KNOCKOUT, false, 0, 0, 0, 4, false, false, false), 6, STRONGER_WINS);
        championship.generate();

        assertThat(championship.progress().phases()).extracting(ChampionshipProgress.PhaseState::status)
                .containsExactly(PhaseStatus.IN_PROGRESS, PhaseStatus.PENDING);
        assertThat(championship.nextStep().action()).isEqualTo(ChampionshipAction.WAITING_RESULTS);
        assertThat(championship.nextStep().pendingMatches()).isEqualTo(15);
    }

    @Test
    void swissKnockout_whenEightTeamsPlayThreeRoundsAndFourQualify_thenTheTopFourPlayTheKnockoutRounds() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.SWISS_KNOCKOUT, false, 3, 0, 0, 4, false, false, false), 8, STRONGER_WINS);

        championship.runToEnd();

        assertThat(championship.matchesOfStage(MatchStage.SWISS)).hasSize(12);
        assertThat(championship.matchesOfStage(MatchStage.KNOCKOUT)).hasSize(3);
        assertThat(championship.progress().champion()).contains(championship.teamIds().get(0));
    }

    // ─── Guards ─────────────────────────────────────────────────────────────────

    @Test
    void planNext_whenTheLastRoundHasNoResultsYet_thenItRefusesToGenerateTheNextOne() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.KNOCKOUT, false, 0, 0, 0, 0, false, false, false), 4, STRONGER_WINS);
        championship.generate();

        org.assertj.core.api.Assertions.assertThatThrownBy(championship::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ChampionshipAction.WAITING_RESULTS.name());
    }

    @Test
    void planNext_whenTheChampionshipIsOver_thenThereIsNothingToGenerate() {
        ChampionshipSimulator championship = new ChampionshipSimulator(
                rules(EventFormatStructure.ROUND_ROBIN, false, 0, 0, 0, 0, false, false, false), 3, STRONGER_WINS);
        championship.runToEnd();

        org.assertj.core.api.Assertions.assertThatThrownBy(championship::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ChampionshipAction.FINISHED.name());
    }

    @Test
    void planNext_whenTheGroupsWereNotDrawn_thenItRefusesToGenerateTheGroupStage() {
        ChampionshipRules rules = rules(EventFormatStructure.GROUPS_KNOCKOUT, false, 0, 2, 2, 0, false, false, false);
        List<TeamRef> undrawn = EngineFixtures.teams(8).stream().map(id -> new TeamRef(id, "T", null, null)).toList();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ChampionshipPlanner.planNext(rules, undrawn, List.of(), java.util.function.UnaryOperator.identity()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GROUPS_NOT_DRAWN");
    }
}
