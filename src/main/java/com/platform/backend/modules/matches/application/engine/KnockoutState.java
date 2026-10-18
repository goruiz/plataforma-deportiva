package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Where a single-elimination bracket stands, worked out from the seeds and the matches played so far. The bracket
 * is never stored: tie {@code t} of round {@code r + 1} is between the winners of ties {@code 2t - 1} and {@code 2t}
 * of round {@code r}, and in the first round a seed without an opponent (a bye) advances without playing.
 */
public final class KnockoutState {

    /** A tie between two teams: one match, or two when it is played home and away. */
    public record Tie(MatchStage stage, int round, int tieNumber, UUID teamA, UUID teamB, boolean bye,
                      List<MatchRef> legs, TieResolver.Outcome outcome) {

        public boolean isDecided() {
            return outcome.isDecided();
        }
    }

    private final int teamCount;
    private final Map<Integer, UUID> teamBySeed = new HashMap<>();
    private final List<Integer> seedOrder;
    private final List<MatchRef> matches;

    private KnockoutState(Map<UUID, Integer> seedByTeam, List<MatchRef> matches) {
        seedByTeam.forEach((team, seed) -> teamBySeed.put(seed, team));
        this.teamCount = seedByTeam.size();
        this.seedOrder = teamCount < 2 ? List.of() : KnockoutBracket.seedOrder(KnockoutBracket.bracketSize(teamCount));
        this.matches = matches;
    }

    /** @param matches the matches of the knockout rounds (the first-to-last rounds and the third-place match) */
    public static KnockoutState of(Map<UUID, Integer> seedByTeam, List<MatchRef> matches) {
        return new KnockoutState(seedByTeam, matches);
    }

    public int teamCount() { return teamCount; }

    public int totalRounds() {
        return teamCount < 2 ? 0 : KnockoutBracket.roundCount(teamCount);
    }

    /** The last round that has matches (0 when none has been generated). */
    public int generatedRounds() {
        return matches.stream()
                .filter(match -> match.stage() == MatchStage.KNOCKOUT)
                .mapToInt(MatchRef::roundNumber)
                .max()
                .orElse(0);
    }

    /** The ties of a round whose teams are already known: all of them once the previous round is decided. */
    public List<Tie> ties(int round) {
        if (round < 1 || round > totalRounds()) {
            return List.of();
        }
        return IntStream.rangeClosed(1, KnockoutBracket.tiesInRound(teamCount, round))
                .mapToObj(tieNumber -> tie(round, tieNumber))
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<Tie> tie(int round, int tieNumber) {
        Optional<UUID> teamA = participantAt(round, 2 * tieNumber - 1);
        Optional<UUID> teamB = participantAt(round, 2 * tieNumber);
        if (round == 1 && teamA.isPresent() != teamB.isPresent()) {
            UUID alone = teamA.orElseGet(teamB::orElseThrow);
            return Optional.of(new Tie(MatchStage.KNOCKOUT, round, tieNumber, alone, null, true, List.of(), TieResolver.byeFor(alone)));
        }
        if (teamA.isEmpty() || teamB.isEmpty()) {
            return Optional.empty();
        }
        List<MatchRef> legs = legsOf(MatchStage.KNOCKOUT, round, tieNumber);
        return Optional.of(new Tie(MatchStage.KNOCKOUT, round, tieNumber, teamA.get(), teamB.get(), false, legs,
                TieResolver.resolve(teamA.get(), teamB.get(), legs)));
    }

    public Optional<UUID> winnerOf(int round, int tieNumber) {
        return tie(round, tieNumber)
                .filter(Tie::isDecided)
                .map(tie -> tie.outcome().winnerTeamId());
    }

    public boolean isRoundDecided(int round) {
        List<Tie> ties = ties(round);
        return ties.size() == KnockoutBracket.tiesInRound(teamCount, round) && ties.stream().allMatch(Tie::isDecided);
    }

    public Optional<UUID> champion() {
        return totalRounds() == 0 ? Optional.empty() : winnerOf(totalRounds(), 1);
    }

    /**
     * The match for the third place, between the two teams that lost the semi-finals. It exists only when both
     * semi-finals were really played (a bye leaves no loser) and have been decided.
     */
    public Optional<Tie> thirdPlace() {
        int semiFinalRound = totalRounds() - 1;
        if (semiFinalRound < 1) {
            return Optional.empty();
        }
        Optional<Tie> first = tie(semiFinalRound, 1).filter(tie -> !tie.bye() && tie.isDecided());
        Optional<Tie> second = tie(semiFinalRound, 2).filter(tie -> !tie.bye() && tie.isDecided());
        if (first.isEmpty() || second.isEmpty()) {
            return Optional.empty();
        }
        UUID firstLoser = first.get().outcome().loserTeamId();
        UUID secondLoser = second.get().outcome().loserTeamId();
        List<MatchRef> legs = legsOf(MatchStage.THIRD_PLACE, totalRounds(), 2);
        return Optional.of(new Tie(MatchStage.THIRD_PLACE, totalRounds(), 2, firstLoser, secondLoser, false, legs,
                TieResolver.resolve(firstLoser, secondLoser, legs)));
    }

    /** Who stands in a place of a round: in the first round a seed of the bracket, later the winner of the tie before. */
    private Optional<UUID> participantAt(int round, int place) {
        if (round == 1) {
            return Optional.ofNullable(teamBySeed.get(seedOrder.get(place - 1)));
        }
        return winnerOf(round - 1, place);
    }

    private List<MatchRef> legsOf(MatchStage stage, int round, int tieNumber) {
        return matches.stream()
                .filter(match -> match.stage() == stage
                        && match.roundNumber() != null && match.roundNumber() == round
                        && match.tieNumber() != null && match.tieNumber() == tieNumber)
                .sorted(Comparator.comparingInt(match -> match.legNumber() == null ? 0 : match.legNumber()))
                .toList();
    }
}
