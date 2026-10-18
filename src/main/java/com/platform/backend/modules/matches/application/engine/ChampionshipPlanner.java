package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.exception.ChampionshipError;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Decides which matches the championship needs next. It only creates what the current phase allows: the whole
 * calendar of a league or of the group stage, or the next round of the Swiss system, of the knockout rounds or of the
 * double elimination (which needs the previous round to be fully decided). The matches are then given dates by the scheduler.
 */
public final class ChampionshipPlanner {

    /**
     * @param drafts the matches to create, in the order they should be scheduled
     * @param seeds  the places given to the teams when an elimination starts (empty otherwise)
     */
    public record Plan(List<MatchDraft> drafts, Map<UUID, Integer> seeds) {
        static Plan of(List<MatchDraft> drafts) {
            return new Plan(drafts, Map.of());
        }
    }

    private ChampionshipPlanner() {}

    /**
     * @param shuffler decides the order of a draw (a test passes an identity function to get a known result)
     * @throws IllegalStateException with the {@link ChampionshipAction} that explains why nothing can be generated now
     */
    public static Plan planNext(ChampionshipRules rules, List<TeamRef> teams, List<MatchRef> matches,
                                UnaryOperator<List<UUID>> shuffler) {
        ChampionshipProgress progress = ChampionshipProgress.of(rules, teams, matches);
        ChampionshipProgress.NextStep step = progress.nextStep();
        if (step.action() != ChampionshipAction.GENERATE_CALENDAR && step.action() != ChampionshipAction.GENERATE_ROUND) {
            throw new IllegalStateException(step.action().name());
        }
        return switch (step.phase()) {
            case LEAGUE -> Plan.of(leagueDrafts(rules, teams));
            case GROUPS -> Plan.of(groupDrafts(rules, progress));
            case SWISS -> Plan.of(swissDrafts(progress, teams, step.roundNumber()));
            case KNOCKOUT -> planKnockout(rules, teams, progress, shuffler);
            case DOUBLE_ELIMINATION -> planDoubleElimination(rules, teams, progress, shuffler);
        };
    }

    // ------------------------------------------------------------------ league, groups, Swiss

    private static List<MatchDraft> leagueDrafts(ChampionshipRules rules, List<TeamRef> teams) {
        return RoundRobinSchedule.rounds(idsOf(teams), rules.roundTrip()).stream()
                .flatMap(round -> round.pairings().stream().map(pairing -> MatchDraft.of(pairing, MatchStage.LEAGUE, round.number())))
                .toList();
    }

    /** Every group plays its own calendar; matchday 1 of all the groups goes first, then matchday 2... */
    private static List<MatchDraft> groupDrafts(ChampionshipRules rules, ChampionshipProgress progress) {
        if (!progress.areGroupsDrawn()) {
            throw new IllegalStateException(ChampionshipError.GROUPS_NOT_DRAWN.name());
        }
        List<MatchDraft> drafts = new ArrayList<>();
        for (int group : progress.groupNumbers()) {
            for (RoundRobinSchedule.Round round : RoundRobinSchedule.rounds(progress.teamIdsOfGroup(group), rules.roundTrip())) {
                round.pairings().forEach(pairing -> drafts.add(MatchDraft.of(pairing, MatchStage.GROUP, round.number()).inGroup(group)));
            }
        }
        return drafts.stream()
                .sorted(Comparator.comparingInt(MatchDraft::roundNumber).thenComparingInt(MatchDraft::groupNumber))
                .toList();
    }

    /** The first round pairs the top half against the bottom half; the next ones pair teams with similar points. */
    private static List<MatchDraft> swissDrafts(ChampionshipProgress progress, List<TeamRef> teams, int round) {
        List<MatchRef> swissMatches = progress.matchesOf(ChampionshipPhase.SWISS);
        List<Pairing> pairings = round == 1
                ? foldedPairings(idsOf(teams))
                : PairingSearch.pair(rankedTeamIds(progress, ChampionshipPhase.SWISS), pairsAlreadyPlayed(swissMatches));

        Map<UUID, Integer> homeGames = homeGamesOf(swissMatches);
        return pairings.stream()
                .map(pairing -> withFewerHomeGamesAtHome(pairing, homeGames))
                .map(pairing -> MatchDraft.of(pairing, MatchStage.SWISS, round))
                .toList();
    }

    private static List<Pairing> foldedPairings(List<UUID> teams) {
        int half = teams.size() / 2;
        return IntStream.range(0, half).mapToObj(index -> new Pairing(teams.get(index), teams.get(index + half))).toList();
    }

    private static Pairing withFewerHomeGamesAtHome(Pairing pairing, Map<UUID, Integer> homeGames) {
        int homeCount = homeGames.getOrDefault(pairing.homeTeamId(), 0);
        int awayCount = homeGames.getOrDefault(pairing.awayTeamId(), 0);
        return awayCount < homeCount ? pairing.reversed() : pairing;
    }

    private static Map<UUID, Integer> homeGamesOf(List<MatchRef> matches) {
        Map<UUID, Integer> homeGames = new HashMap<>();
        matches.forEach(match -> homeGames.merge(match.homeTeamId(), 1, Integer::sum));
        return homeGames;
    }

    // ------------------------------------------------------------------ knockout rounds

    private static Plan planKnockout(ChampionshipRules rules, List<TeamRef> teams, ChampionshipProgress progress,
                                     UnaryOperator<List<UUID>> shuffler) {
        if (progress.matchesOf(ChampionshipPhase.KNOCKOUT).isEmpty()) {
            return startKnockout(rules, teams, progress, shuffler);
        }
        return Plan.of(nextKnockoutRound(rules, teams, progress));
    }

    /** Seeds the qualified teams (the best first) and creates the first round; the best seeds may get a bye. */
    private static Plan startKnockout(ChampionshipRules rules, List<TeamRef> teams, ChampionshipProgress progress,
                                      UnaryOperator<List<UUID>> shuffler) {
        List<UUID> bySeed = qualifiedTeamsBySeed(rules, teams, progress, shuffler);
        Map<UUID, Integer> seeds = seedsOf(bySeed);

        List<MatchDraft> drafts = new ArrayList<>();
        for (KnockoutBracket.FirstRoundTie tie : KnockoutBracket.firstRound(bySeed.size())) {
            if (!tie.isBye()) {
                drafts.addAll(tieDrafts(bySeed.get(tie.betterSeed() - 1), bySeed.get(tie.worseSeed() - 1), seeds,
                        MatchStage.KNOCKOUT, 1, tie.tieNumber(), rules.knockoutRoundTrip()));
            }
        }
        return new Plan(inSchedulingOrder(drafts), seeds);
    }

    private static List<MatchDraft> nextKnockoutRound(ChampionshipRules rules, List<TeamRef> teams, ChampionshipProgress progress) {
        KnockoutState state = progress.knockoutState();
        int round = state.generatedRounds() + 1;
        if (round > state.totalRounds() || !state.isRoundDecided(round - 1)) {
            throw new IllegalStateException(ChampionshipAction.WAITING_RESULTS.name());
        }
        Map<UUID, Integer> seeds = seedsOfTeams(teams);

        List<MatchDraft> drafts = new ArrayList<>();
        for (KnockoutState.Tie tie : state.ties(round)) {
            drafts.addAll(tieDrafts(tie.teamA(), tie.teamB(), seeds, MatchStage.KNOCKOUT, round, tie.tieNumber(), rules.knockoutRoundTrip()));
        }
        if (round == state.totalRounds() && rules.thirdPlaceMatch()) {
            state.thirdPlace().filter(tie -> tie.legs().isEmpty()).ifPresent(tie -> drafts.addAll(
                    tieDrafts(tie.teamA(), tie.teamB(), seeds, MatchStage.THIRD_PLACE, round, tie.tieNumber(), rules.knockoutRoundTrip())));
        }
        return inSchedulingOrder(drafts);
    }

    /** Whoever comes first of the previous phase is the best seed: the group winners, then the runners-up... */
    private static List<UUID> qualifiedTeamsBySeed(ChampionshipRules rules, List<TeamRef> teams, ChampionshipProgress progress,
                                                   UnaryOperator<List<UUID>> shuffler) {
        List<ChampionshipPhase> phases = rules.structure().phases();
        int knockoutIndex = phases.indexOf(ChampionshipPhase.KNOCKOUT);
        if (knockoutIndex == 0) {
            return shuffler.apply(idsOf(teams));
        }
        ChampionshipPhase previous = phases.get(knockoutIndex - 1);
        if (previous == ChampionshipPhase.GROUPS) {
            return qualifiersFromGroups(rules, progress);
        }
        return rankedTeamIds(progress, previous).stream().limit(rules.playoffTeams()).toList();
    }

    private static List<UUID> qualifiersFromGroups(ChampionshipRules rules, ChampionshipProgress progress) {
        List<ChampionshipProgress.TableView> groupTables = progress.tablesOf(ChampionshipPhase.GROUPS);
        List<UUID> qualified = new ArrayList<>();
        for (int position = 1; position <= rules.teamsAdvancingPerGroup(); position++) {
            int currentPosition = position;
            groupTables.stream()
                    .flatMap(table -> table.rows().stream().filter(row -> row.position() == currentPosition)
                            .map(row -> new GroupQualifier(table.groupNumber(), row)))
                    .sorted(GroupQualifier.COMPARATOR)
                    .forEach(qualifier -> qualified.add(qualifier.row().teamId()));
        }
        return qualified;
    }

    private record GroupQualifier(int groupNumber, StandingRow row) {
        static final Comparator<GroupQualifier> COMPARATOR = Comparator
                .<GroupQualifier>comparingInt(qualifier -> qualifier.row().points()).reversed()
                .thenComparing(Comparator.<GroupQualifier>comparingInt(qualifier -> qualifier.row().goalDifference()).reversed())
                .thenComparing(Comparator.<GroupQualifier>comparingInt(qualifier -> qualifier.row().goalsFor()).reversed())
                .thenComparingInt(GroupQualifier::groupNumber);
    }

    // ------------------------------------------------------------------ double elimination

    private static Plan planDoubleElimination(ChampionshipRules rules, List<TeamRef> teams, ChampionshipProgress progress,
                                              UnaryOperator<List<UUID>> shuffler) {
        if (progress.matchesOf(ChampionshipPhase.DOUBLE_ELIMINATION).isEmpty()) {
            List<UUID> bySeed = shuffler.apply(idsOf(teams));
            Map<UUID, Integer> seeds = seedsOf(bySeed);
            DoubleEliminationState empty = new DoubleEliminationState(bySeed, rules.grandFinalReset(), List.of());
            return new Plan(doubleEliminationRound(rules, empty, seeds), seeds);
        }
        return Plan.of(doubleEliminationRound(rules, progress.doubleEliminationState(), seedsOfTeams(teams)));
    }

    private static List<MatchDraft> doubleEliminationRound(ChampionshipRules rules, DoubleEliminationState state,
                                                           Map<UUID, Integer> seeds) {
        int round = state.lastRound() + 1;
        boolean legs = rules.knockoutRoundTrip();
        if (state.needsGrandFinalRematch()) {
            DoubleEliminationState.Tie firstFinal = state.grandFinal().orElseThrow();
            return inSchedulingOrder(tieDrafts(firstFinal.teamA(), firstFinal.teamB(), seeds, MatchStage.GRAND_FINAL, round, 1, legs));
        }
        List<UUID> undefeated = state.aliveWithDefeats(0);
        List<UUID> oneDefeat = state.aliveWithDefeats(1);
        if (state.grandFinal().isEmpty() && undefeated.size() == 1 && oneDefeat.size() == 1) {
            return inSchedulingOrder(tieDrafts(undefeated.get(0), oneDefeat.get(0), seeds, MatchStage.GRAND_FINAL, round, 1, legs));
        }
        Set<Set<UUID>> played = state.ties().stream()
                .map(tie -> Pairing.unorderedKey(tie.teamA(), tie.teamB()))
                .collect(Collectors.toSet());

        List<MatchDraft> drafts = new ArrayList<>();
        drafts.addAll(sideDrafts(undefeated, MatchStage.WINNERS, round, state, played, seeds, legs));
        drafts.addAll(sideDrafts(oneDefeat, MatchStage.LOSERS, round, state, played, seeds, legs));
        return inSchedulingOrder(drafts);
    }

    /** Pairs the teams of one side among themselves; with an odd number, the team that has rested the least sits out. */
    private static List<MatchDraft> sideDrafts(List<UUID> side, MatchStage stage, int round, DoubleEliminationState state,
                                               Set<Set<UUID>> played, Map<UUID, Integer> seeds, boolean legs) {
        if (side.size() < 2) {
            return List.of();
        }
        List<UUID> playing = new ArrayList<>(side);
        if (playing.size() % 2 != 0) {
            playing.remove(teamThatRests(side, state, seeds));
        }
        List<Pairing> pairings = PairingSearch.pair(playing, played);

        List<MatchDraft> drafts = new ArrayList<>();
        int tieNumber = 1;
        for (Pairing pairing : pairings) {
            drafts.addAll(tieDrafts(pairing.homeTeamId(), pairing.awayTeamId(), seeds, stage, round, tieNumber++, legs));
        }
        return drafts;
    }

    private static UUID teamThatRests(List<UUID> side, DoubleEliminationState state, Map<UUID, Integer> seeds) {
        return side.stream()
                .min(Comparator.comparingInt(state::restedRounds).thenComparingInt(team -> seeds.getOrDefault(team, Integer.MAX_VALUE)))
                .orElseThrow();
    }

    // ------------------------------------------------------------------ shared

    /**
     * The matches of a tie. Played once, the better seed is at home. Played home and away, the worse seed hosts the
     * first leg so the better seed has the return leg at home.
     */
    private static List<MatchDraft> tieDrafts(UUID teamX, UUID teamY, Map<UUID, Integer> seeds, MatchStage stage, int round,
                                              int tieNumber, boolean homeAndAway) {
        boolean xIsBetter = seeds.getOrDefault(teamX, Integer.MAX_VALUE) <= seeds.getOrDefault(teamY, Integer.MAX_VALUE);
        UUID better = xIsBetter ? teamX : teamY;
        UUID worse = xIsBetter ? teamY : teamX;
        if (!homeAndAway) {
            return List.of(new MatchDraft(better, worse, stage, round, null, tieNumber, null));
        }
        return List.of(new MatchDraft(worse, better, stage, round, null, tieNumber, 1),
                new MatchDraft(better, worse, stage, round, null, tieNumber, 2));
    }

    /** First legs (and single matches) go first, so a return leg is always scheduled after its first leg. */
    private static List<MatchDraft> inSchedulingOrder(List<MatchDraft> drafts) {
        return drafts.stream()
                .sorted(Comparator.comparingInt((MatchDraft draft) -> draft.legNumber() == null ? 1 : draft.legNumber()))
                .toList();
    }

    private static List<UUID> rankedTeamIds(ChampionshipProgress progress, ChampionshipPhase phase) {
        return progress.tablesOf(phase).get(0).rows().stream().map(StandingRow::teamId).toList();
    }

    private static Set<Set<UUID>> pairsAlreadyPlayed(List<MatchRef> matches) {
        return matches.stream()
                .map(match -> Pairing.unorderedKey(match.homeTeamId(), match.awayTeamId()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static List<UUID> idsOf(List<TeamRef> teams) {
        return teams.stream().map(TeamRef::id).toList();
    }

    private static Map<UUID, Integer> seedsOf(List<UUID> bySeed) {
        Map<UUID, Integer> seeds = new HashMap<>();
        for (int index = 0; index < bySeed.size(); index++) {
            seeds.put(bySeed.get(index), index + 1);
        }
        return seeds;
    }

    private static Map<UUID, Integer> seedsOfTeams(List<TeamRef> teams) {
        return teams.stream()
                .filter(team -> team.seed() != null)
                .collect(Collectors.toMap(TeamRef::id, TeamRef::seed));
    }
}
