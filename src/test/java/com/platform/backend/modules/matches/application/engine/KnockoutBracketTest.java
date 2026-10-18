package com.platform.backend.modules.matches.application.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnockoutBracketTest {

    @Test
    void bracketSize_whenTeamsAreNotAPowerOfTwo_thenRoundsUpToTheNextOne() {
        assertThat(KnockoutBracket.bracketSize(2)).isEqualTo(2);
        assertThat(KnockoutBracket.bracketSize(3)).isEqualTo(4);
        assertThat(KnockoutBracket.bracketSize(8)).isEqualTo(8);
        assertThat(KnockoutBracket.bracketSize(9)).isEqualTo(16);
        assertThat(KnockoutBracket.bracketSize(33)).isEqualTo(64);
    }

    @Test
    void roundCount_whenBracketHasEightPlaces_thenThereAreThreeRounds() {
        assertThat(KnockoutBracket.roundCount(8)).isEqualTo(3);
        assertThat(KnockoutBracket.roundCount(5)).isEqualTo(3);
        assertThat(KnockoutBracket.roundCount(2)).isEqualTo(1);
    }

    @Test
    void tiesInRound_whenEightTeams_thenFourThenTwoThenOne() {
        assertThat(KnockoutBracket.tiesInRound(8, 1)).isEqualTo(4);
        assertThat(KnockoutBracket.tiesInRound(8, 2)).isEqualTo(2);
        assertThat(KnockoutBracket.tiesInRound(8, 3)).isEqualTo(1);
    }

    @Test
    void roundsFromEnd_whenEightTeams_thenTheLastRoundIsTheFinal() {
        assertThat(KnockoutBracket.roundsFromEnd(8, 3)).isZero();
        assertThat(KnockoutBracket.roundsFromEnd(8, 2)).isEqualTo(1);
        assertThat(KnockoutBracket.roundsFromEnd(8, 1)).isEqualTo(2);
    }

    @Test
    void seedOrder_whenEightPlaces_thenTheBestSeedsCanOnlyMeetInTheLastRounds() {
        assertThat(KnockoutBracket.seedOrder(2)).containsExactly(1, 2);
        assertThat(KnockoutBracket.seedOrder(4)).containsExactly(1, 4, 2, 3);
        assertThat(KnockoutBracket.seedOrder(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }

    @Test
    void firstRound_whenEightTeams_thenTheBestMeetsTheWorst() {
        List<KnockoutBracket.FirstRoundTie> ties = KnockoutBracket.firstRound(8);

        assertThat(ties).extracting(KnockoutBracket.FirstRoundTie::betterSeed).containsExactly(1, 4, 2, 3);
        assertThat(ties).extracting(KnockoutBracket.FirstRoundTie::worseSeed).containsExactly(8, 5, 7, 6);
        assertThat(ties).noneMatch(KnockoutBracket.FirstRoundTie::isBye);
    }

    @Test
    void firstRound_whenSixTeams_thenTheTwoBestSeedsHaveAByeAndTheRestPlay() {
        List<KnockoutBracket.FirstRoundTie> ties = KnockoutBracket.firstRound(6);

        assertThat(ties).hasSize(4);
        assertThat(ties.get(0).isBye()).isTrue();
        assertThat(ties.get(0).betterSeed()).isEqualTo(1);
        assertThat(ties.get(2).isBye()).isTrue();
        assertThat(ties.get(2).betterSeed()).isEqualTo(2);
        assertThat(ties.get(1).worseSeed()).isEqualTo(5);
        assertThat(ties.get(3).worseSeed()).isEqualTo(6);
    }

    @Test
    void firstRound_whenThreeTeams_thenOnlyTheSecondAndThirdSeedsPlay() {
        List<KnockoutBracket.FirstRoundTie> ties = KnockoutBracket.firstRound(3);

        assertThat(ties).hasSize(2);
        assertThat(ties.get(0).isBye()).isTrue();
        assertThat(ties.get(1).betterSeed()).isEqualTo(2);
        assertThat(ties.get(1).worseSeed()).isEqualTo(3);
    }

    @Test
    void firstRound_whenTwoTeams_thenThereIsOneTie() {
        List<KnockoutBracket.FirstRoundTie> ties = KnockoutBracket.firstRound(2);

        assertThat(ties).hasSize(1);
        assertThat(ties.get(0).isBye()).isFalse();
    }
}
