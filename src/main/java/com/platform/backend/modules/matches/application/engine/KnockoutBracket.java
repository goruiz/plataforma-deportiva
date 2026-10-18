package com.platform.backend.modules.matches.application.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a single-elimination bracket. The bracket always has a power of two of places; when there are fewer
 * teams, the best seeds get a bye (they skip the first round). Seed 1 is the best team.
 */
public final class KnockoutBracket {

    private KnockoutBracket() {}

    /** A tie of the first round; {@code worseSeed} is null when the better seed has a bye. */
    public record FirstRoundTie(int tieNumber, int betterSeed, Integer worseSeed) {
        public boolean isBye() {
            return worseSeed == null;
        }
    }

    /** The number of places of the bracket: the smallest power of two that fits the teams (at least 2). */
    public static int bracketSize(int teams) {
        int size = 2;
        while (size < teams) {
            size *= 2;
        }
        return size;
    }

    public static int roundCount(int teams) {
        return Integer.numberOfTrailingZeros(bracketSize(teams));
    }

    /** How many ties a round has: half the bracket in the first round, half of that in the next... */
    public static int tiesInRound(int teams, int round) {
        return bracketSize(teams) >> round;
    }

    /** 0 for the final, 1 for the semi-finals, 2 for the quarter-finals... */
    public static int roundsFromEnd(int teams, int round) {
        return roundCount(teams) - round;
    }

    /**
     * The seeds in the order of the bracket places, so that the best seeds can only meet in the last rounds:
     * 1 and 2 in the final, 1 to 4 in the semi-finals...
     */
    public static List<Integer> seedOrder(int size) {
        List<Integer> order = new ArrayList<>(List.of(1));
        while (order.size() < size) {
            int nextSize = order.size() * 2;
            List<Integer> expanded = new ArrayList<>();
            for (int seed : order) {
                expanded.add(seed);
                expanded.add(nextSize + 1 - seed);
            }
            order = expanded;
        }
        return order;
    }

    public static List<FirstRoundTie> firstRound(int teams) {
        List<Integer> order = seedOrder(bracketSize(teams));
        List<FirstRoundTie> ties = new ArrayList<>();
        for (int tie = 1; tie <= order.size() / 2; tie++) {
            int first = order.get(2 * tie - 2);
            int second = order.get(2 * tie - 1);
            int better = Math.min(first, second);
            int worse = Math.max(first, second);
            ties.add(new FirstRoundTie(tie, better, worse > teams ? null : worse));
        }
        return ties;
    }
}
