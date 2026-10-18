package com.platform.backend.modules.matches.application.engine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.platform.backend.modules.matches.application.engine.EngineFixtures.teams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PairingSearchTest {

    @Test
    void pair_whenNobodyHasPlayed_thenTheBestRankedTeamMeetsTheNextOne() {
        List<UUID> ranked = teams(4);

        List<Pairing> pairings = PairingSearch.pair(ranked, Set.of());

        assertThat(pairings).containsExactly(new Pairing(ranked.get(0), ranked.get(1)), new Pairing(ranked.get(2), ranked.get(3)));
    }

    @Test
    void pair_whenTwoTeamsAlreadyMet_thenEachMeetsTheBestRankedTeamItHasNotPlayed() {
        List<UUID> ranked = teams(4);
        Set<Set<UUID>> played = Set.of(Pairing.unorderedKey(ranked.get(0), ranked.get(1)));

        List<Pairing> pairings = PairingSearch.pair(ranked, played);

        assertThat(pairings).containsExactly(new Pairing(ranked.get(0), ranked.get(2)), new Pairing(ranked.get(1), ranked.get(3)));
    }

    @Test
    void pair_whenTheGreedyChoiceLeavesSomeoneWithoutRival_thenItBacksUpAndFindsAnotherCombination() {
        List<UUID> ranked = teams(4);
        // 0 can only meet 2 or 3; if 0 takes 2, then 1 and 3 have already met: the search must give 0 the 3.
        Set<Set<UUID>> played = Set.of(
                Pairing.unorderedKey(ranked.get(0), ranked.get(1)),
                Pairing.unorderedKey(ranked.get(1), ranked.get(3)));

        List<Pairing> pairings = PairingSearch.pair(ranked, played);

        Set<Set<UUID>> result = new HashSet<>();
        pairings.forEach(pairing -> result.add(pairing.unorderedKey()));
        assertThat(result).doesNotContainAnyElementsOf(played);
        assertThat(pairings).hasSize(2);
    }

    @Test
    void pair_whenEveryCombinationRepeatsAMatch_thenRematchesAreAllowed() {
        List<UUID> ranked = teams(2);
        Set<Set<UUID>> played = Set.of(Pairing.unorderedKey(ranked.get(0), ranked.get(1)));

        List<Pairing> pairings = PairingSearch.pair(ranked, played);

        assertThat(pairings).containsExactly(new Pairing(ranked.get(0), ranked.get(1)));
    }

    @Test
    void pair_whenThereIsAnOddNumberOfTeams_thenItFails() {
        assertThatThrownBy(() -> PairingSearch.pair(teams(3), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pair_whenManyTeamsHavePlayedManyRounds_thenItStillFindsAPairingWithoutRematches() {
        List<UUID> ranked = teams(16);
        // The first five rounds of a round robin have been played.
        Set<Set<UUID>> played = new HashSet<>();
        RoundRobinSchedule.rounds(ranked, false).stream().limit(5)
                .forEach(round -> round.pairings().forEach(pairing -> played.add(pairing.unorderedKey())));

        List<Pairing> pairings = PairingSearch.pair(ranked, played);

        assertThat(pairings).hasSize(8);
        pairings.forEach(pairing -> assertThat(played).doesNotContain(pairing.unorderedKey()));
    }
}
