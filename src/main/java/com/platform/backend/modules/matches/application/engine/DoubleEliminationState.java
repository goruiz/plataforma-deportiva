package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Where a double elimination stands, worked out from the matches played. A team is out after its second defeat.
 * Each round pairs the teams that still have no defeat among themselves (the winners' side) and the teams with one
 * defeat among themselves (the losers' side), so every team plays against rivals with its own record. When one team
 * is left in each side they meet in the grand final; if the team that came from the losers' side wins it and the
 * format says so, a second final is played, because that team has not lost twice yet.
 */
public final class DoubleEliminationState {

    private static final int DEFEATS_TO_BE_ELIMINATED = 2;

    /** A tie of one side (or of the final): one match, or two when it is played home and away. */
    public record Tie(MatchStage stage, int round, int tieNumber, UUID teamA, UUID teamB, List<MatchRef> legs,
                      TieResolver.Outcome outcome) {

        public boolean isDecided() {
            return outcome.isDecided();
        }
    }

    private static final Comparator<MatchStage> STAGE_ORDER = Comparator.comparingInt(MatchStage::ordinal);

    private final List<UUID> teamsBySeed;
    private final boolean grandFinalReset;
    private final List<Tie> ties;
    /** Defeats before the grand final: that is what tells which finalist comes from which side. */
    private final Map<UUID, Integer> defeats = new HashMap<>();
    private final Map<UUID, Integer> restedRounds = new HashMap<>();

    /**
     * @param teamsBySeed every team of the elimination, best seed first
     * @param matches     the matches of the double elimination
     */
    public DoubleEliminationState(List<UUID> teamsBySeed, boolean grandFinalReset, List<MatchRef> matches) {
        this.teamsBySeed = teamsBySeed;
        this.grandFinalReset = grandFinalReset;
        this.ties = buildTies(matches);
        replayRounds();
    }

    public List<Tie> ties() { return ties; }

    public int lastRound() {
        return ties.stream().mapToInt(Tie::round).max().orElse(0);
    }

    public boolean isRoundDecided(int round) {
        List<Tie> roundTies = tiesOfRound(round);
        return !roundTies.isEmpty() && roundTies.stream().allMatch(Tie::isDecided);
    }

    private List<Tie> tiesOfRound(int round) {
        return ties.stream().filter(tie -> tie.round() == round).toList();
    }

    /** The alive teams with exactly {@code defeatCount} defeats, best seed first. */
    public List<UUID> aliveWithDefeats(int defeatCount) {
        return teamsBySeed.stream()
                .filter(team -> defeats.getOrDefault(team, 0) == defeatCount)
                .toList();
    }

    /** The rounds a team has sat out while it was still alive: the team that rested the least rests next. */
    public int restedRounds(UUID team) {
        return restedRounds.getOrDefault(team, 0);
    }

    public Optional<Tie> grandFinal() {
        return grandFinalTies().stream().findFirst();
    }

    public Optional<Tie> grandFinalRematch() {
        return grandFinalTies().stream().skip(1).findFirst();
    }

    public Optional<UUID> champion() {
        Optional<Tie> firstFinal = grandFinal().filter(Tie::isDecided);
        if (firstFinal.isEmpty()) {
            return Optional.empty();
        }
        UUID winner = firstFinal.get().outcome().winnerTeamId();
        boolean cameFromTheLosersSide = defeats.getOrDefault(winner, 0) > 0;
        if (!cameFromTheLosersSide || !grandFinalReset) {
            return Optional.of(winner);
        }
        return grandFinalRematch().filter(Tie::isDecided).map(rematch -> rematch.outcome().winnerTeamId());
    }

    /** True when the first final was won from the losers' side and the format asks for a second one that is not created yet. */
    public boolean needsGrandFinalRematch() {
        Optional<Tie> firstFinal = grandFinal().filter(Tie::isDecided);
        if (firstFinal.isEmpty() || !grandFinalReset || grandFinalRematch().isPresent()) {
            return false;
        }
        return defeats.getOrDefault(firstFinal.get().outcome().winnerTeamId(), 0) > 0;
    }

    private List<Tie> grandFinalTies() {
        return ties.stream().filter(tie -> tie.stage() == MatchStage.GRAND_FINAL).toList();
    }

    private List<Tie> buildTies(List<MatchRef> matches) {
        Map<List<Object>, List<MatchRef>> legsByTie = new LinkedHashMap<>();
        for (MatchRef match : matches) {
            legsByTie.computeIfAbsent(List.of(match.stage(), match.roundNumber(), match.tieNumber()), key -> new ArrayList<>()).add(match);
        }
        return legsByTie.values().stream()
                .map(this::toTie)
                .sorted(Comparator.comparingInt(Tie::round).thenComparing(Tie::stage, STAGE_ORDER).thenComparingInt(Tie::tieNumber))
                .toList();
    }

    private Tie toTie(List<MatchRef> legs) {
        List<MatchRef> ordered = legs.stream()
                .sorted(Comparator.comparingInt(leg -> leg.legNumber() == null ? 0 : leg.legNumber()))
                .toList();
        MatchRef first = ordered.get(0);
        return new Tie(first.stage(), first.roundNumber(), first.tieNumber(), first.homeTeamId(), first.awayTeamId(), ordered,
                TieResolver.resolve(first.homeTeamId(), first.awayTeamId(), ordered));
    }

    /** Goes round by round counting defeats, and the rounds each team was alive but did not play. */
    private void replayRounds() {
        for (int round = 1; round <= lastRound(); round++) {
            List<Tie> roundTies = tiesOfRound(round);
            countRestedRounds(roundTies);
            roundTies.stream()
                    .filter(tie -> tie.stage() != MatchStage.GRAND_FINAL && tie.isDecided())
                    .forEach(tie -> defeats.merge(tie.outcome().loserTeamId(), 1, Integer::sum));
        }
    }

    private void countRestedRounds(List<Tie> roundTies) {
        for (UUID team : teamsBySeed) {
            boolean alive = defeats.getOrDefault(team, 0) < DEFEATS_TO_BE_ELIMINATED;
            boolean played = roundTies.stream().anyMatch(tie -> team.equals(tie.teamA()) || team.equals(tie.teamB()));
            if (alive && !played) {
                restedRounds.merge(team, 1, Integer::sum);
            }
        }
    }
}
