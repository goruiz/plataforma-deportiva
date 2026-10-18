package com.platform.backend.modules.matches.application.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Who plays whom on each matchday when everyone plays everyone (circle method). */
public final class RoundRobinSchedule {

    private RoundRobinSchedule() {}

    public record Round(int number, List<Pairing> pairings) {
    }

    /**
     * Every team meets every other once; with {@code roundTrip} the whole first half is played again with the venues
     * swapped, as the second half of the season. With an odd number of teams one team rests each matchday.
     */
    public static List<Round> rounds(List<UUID> teams, boolean roundTrip) {
        List<Round> rounds = new ArrayList<>(firstLegRounds(teams));
        if (roundTrip) {
            int firstLegRoundCount = rounds.size();
            for (int index = 0; index < firstLegRoundCount; index++) {
                Round firstLeg = rounds.get(index);
                rounds.add(new Round(firstLegRoundCount + firstLeg.number(),
                        firstLeg.pairings().stream().map(Pairing::reversed).toList()));
            }
        }
        return rounds;
    }

    private static List<Round> firstLegRounds(List<UUID> teams) {
        List<UUID> rotation = new ArrayList<>(teams);
        if (rotation.size() % 2 != 0) {
            rotation.add(null); // the team paired with null rests that matchday
        }
        int size = rotation.size();
        List<Round> rounds = new ArrayList<>();

        for (int round = 0; round < size - 1; round++) {
            List<Pairing> pairings = new ArrayList<>();
            for (int index = 0; index < size / 2; index++) {
                UUID first = rotation.get(index);
                UUID second = rotation.get(size - 1 - index);
                if (first != null && second != null) {
                    pairings.add(isSwapped(round) ? new Pairing(second, first) : new Pairing(first, second));
                }
            }
            rounds.add(new Round(round + 1, pairings));
            rotation.add(1, rotation.remove(size - 1)); // the first team stays, the others rotate
        }
        return rounds;
    }

    /**
     * Swaps the venues of every pairing on the odd matchdays. A team stays on the same side of the pairings for several
     * matchdays while it rotates, so this makes it alternate home and away matchday after matchday.
     */
    private static boolean isSwapped(int round) {
        return round % 2 == 1;
    }
}
