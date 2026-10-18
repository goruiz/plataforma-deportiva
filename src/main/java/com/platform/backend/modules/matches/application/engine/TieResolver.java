package com.platform.backend.modules.matches.application.engine;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Decides who wins a tie between two teams: the one with more goals over its one or two matches. A tie that ends
 * level has no winner until an administrator says which team advances (the penalty shoot-out is not recorded as goals).
 */
public final class TieResolver {

    private TieResolver() {}

    public enum Status {
        /** The matches of the tie have no result yet (or have not been generated). */
        PENDING,
        /** Level on goals, and nobody has been named as the team that advances. */
        NEEDS_TIEBREAK,
        DECIDED
    }

    /** The aggregates are in the order the teams were given: {@code teamA} first. */
    public record Outcome(Status status, UUID winnerTeamId, UUID loserTeamId, int teamAGoals, int teamBGoals) {

        public boolean isDecided() {
            return status == Status.DECIDED;
        }

        static Outcome pending() {
            return new Outcome(Status.PENDING, null, null, 0, 0);
        }
    }

    /** A tie nobody has to play: the only team advances. */
    public static Outcome byeFor(UUID team) {
        return new Outcome(Status.DECIDED, team, null, 0, 0);
    }

    public static Outcome resolve(UUID teamA, UUID teamB, List<MatchRef> legs) {
        if (legs.isEmpty() || legs.stream().anyMatch(leg -> !leg.isDecided())) {
            return Outcome.pending();
        }
        int goalsA = legs.stream().mapToInt(leg -> leg.goalsOf(teamA)).sum();
        int goalsB = legs.stream().mapToInt(leg -> leg.goalsOf(teamB)).sum();
        if (goalsA != goalsB) {
            return goalsA > goalsB ? decided(teamA, teamB, goalsA, goalsB) : decided(teamB, teamA, goalsA, goalsB);
        }
        UUID advancing = decidingLeg(legs).advancingTeamId();
        if (advancing == null || !(advancing.equals(teamA) || advancing.equals(teamB))) {
            return new Outcome(Status.NEEDS_TIEBREAK, null, null, goalsA, goalsB);
        }
        return advancing.equals(teamA) ? decided(teamA, teamB, goalsA, goalsB) : decided(teamB, teamA, goalsA, goalsB);
    }

    /** The match that closes the tie: the return leg, or the only match. */
    public static MatchRef decidingLeg(List<MatchRef> legs) {
        return legs.stream()
                .max(Comparator.comparingInt(leg -> leg.legNumber() == null ? 0 : leg.legNumber()))
                .orElseThrow();
    }

    private static Outcome decided(UUID winner, UUID loser, int goalsA, int goalsB) {
        return new Outcome(Status.DECIDED, winner, loser, goalsA, goalsB);
    }
}
