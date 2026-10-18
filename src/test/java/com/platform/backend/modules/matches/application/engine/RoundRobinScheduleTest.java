package com.platform.backend.modules.matches.application.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.platform.backend.modules.matches.application.engine.EngineFixtures.teams;
import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinScheduleTest {

    @Test
    void rounds_whenThereIsAnEvenNumberOfTeams_thenEveryTeamPlaysOnceInEveryRound() {
        List<UUID> teams = teams(6);

        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams, false);

        assertThat(rounds).hasSize(5);
        for (RoundRobinSchedule.Round round : rounds) {
            assertThat(round.pairings()).hasSize(3);
            Set<UUID> playing = new HashSet<>();
            round.pairings().forEach(pairing -> {
                playing.add(pairing.homeTeamId());
                playing.add(pairing.awayTeamId());
            });
            assertThat(playing).hasSize(6);
        }
    }

    @Test
    void rounds_whenSingleLeg_thenEveryPairMeetsExactlyOnce() {
        List<UUID> teams = teams(7);

        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams, false);

        Map<Set<UUID>, Integer> meetings = countMeetings(rounds);
        assertThat(meetings).hasSize(7 * 6 / 2);
        assertThat(meetings.values()).containsOnly(1);
    }

    @Test
    void rounds_whenThereIsAnOddNumberOfTeams_thenOneTeamRestsInEveryRound() {
        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams(5), false);

        assertThat(rounds).hasSize(5);
        assertThat(rounds).allSatisfy(round -> assertThat(round.pairings()).hasSize(2));
    }

    @Test
    void rounds_whenRoundTrip_thenEveryPairMeetsTwiceOnceAtEachVenue() {
        List<UUID> teams = teams(4);

        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams, true);

        assertThat(rounds).hasSize(6);
        assertThat(rounds).extracting(RoundRobinSchedule.Round::number).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(countMeetings(rounds).values()).containsOnly(2);
        Set<Pairing> venues = new HashSet<>();
        rounds.forEach(round -> venues.addAll(round.pairings()));
        assertThat(venues).hasSize(12);
    }

    @Test
    void rounds_whenRoundTrip_thenTheSecondHalfRepeatsTheFirstWithTheVenuesSwapped() {
        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams(4), true);

        for (int index = 0; index < 3; index++) {
            List<Pairing> firstLeg = rounds.get(index).pairings();
            List<Pairing> returnLeg = rounds.get(index + 3).pairings();
            assertThat(returnLeg).containsExactlyElementsOf(firstLeg.stream().map(Pairing::reversed).toList());
        }
    }

    @Test
    void rounds_whenSingleLegAndEvenTeams_thenNobodyPlaysMuchMoreAtHomeThanAway() {
        List<UUID> teams = teams(8);

        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams, false);

        Map<UUID, Integer> homeGames = new HashMap<>();
        rounds.forEach(round -> round.pairings().forEach(pairing -> homeGames.merge(pairing.homeTeamId(), 1, Integer::sum)));
        teams.forEach(team -> assertThat(homeGames.getOrDefault(team, 0)).isBetween(2, 5));
    }

    @Test
    void rounds_whenThereAreTwoTeams_thenThereIsOneRoundWithOneMatch() {
        List<RoundRobinSchedule.Round> rounds = RoundRobinSchedule.rounds(teams(2), false);

        assertThat(rounds).hasSize(1);
        assertThat(rounds.get(0).pairings()).hasSize(1);
    }

    private Map<Set<UUID>, Integer> countMeetings(List<RoundRobinSchedule.Round> rounds) {
        Map<Set<UUID>, Integer> meetings = new HashMap<>();
        rounds.forEach(round -> round.pairings().forEach(pairing -> meetings.merge(pairing.unorderedKey(), 1, Integer::sum)));
        return meetings;
    }
}
