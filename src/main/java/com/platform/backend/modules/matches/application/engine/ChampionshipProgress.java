package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;
import com.platform.backend.modules.matches.domain.enums.PhaseStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Where a championship stands, worked out only from the format's rules, the registered teams and the matches played:
 * which phase is running, the points tables, the rounds and ties, the champion and what the administrator can do
 * next. Nothing is stored, so it can never disagree with the matches.
 */
public final class ChampionshipProgress {

    private static final int LEAGUE_WITHOUT_MATCHDAY = 0;

    /** A match, or the one or two legs of a tie, between two teams. Winner and tie-break only exist in eliminations. */
    public record TieView(int tieNumber, MatchStage stage, UUID teamAId, UUID teamBId, boolean bye,
                          List<MatchRef> legs, int teamAGoals, int teamBGoals, UUID winnerTeamId, boolean needsTiebreak) {
    }

    public record RoundView(ChampionshipPhase phase, int roundNumber, Integer groupNumber, List<TieView> ties) {
    }

    public record TableView(ChampionshipPhase phase, Integer groupNumber, List<StandingRow> rows) {
    }

    public record PhaseState(ChampionshipPhase phase, PhaseStatus status) {
    }

    /**
     * What to do next.
     *
     * @param roundNumber      the round that would be generated (Swiss and eliminations)
     * @param matchesToResolve the matches whose tie ended level and needs an administrator to say who advances
     */
    public record NextStep(ChampionshipAction action, ChampionshipPhase phase, Integer roundNumber, int pendingMatches,
                           int registeredTeams, int requiredTeams, boolean evenTeamsRequired, List<UUID> matchesToResolve) {
    }

    private final ChampionshipRules rules;
    private final List<TeamRef> teams;
    private final List<MatchRef> matches;
    private final List<PhaseState> phases;

    private ChampionshipProgress(ChampionshipRules rules, List<TeamRef> teams, List<MatchRef> matches) {
        this.rules = rules;
        this.teams = teams;
        this.matches = matches;
        this.phases = computePhases();
    }

    public static ChampionshipProgress of(ChampionshipRules rules, List<TeamRef> teams, List<MatchRef> matches) {
        return new ChampionshipProgress(rules, teams, matches);
    }

    public List<PhaseState> phases() { return phases; }

    /** The phase being played: the first one that has not finished; null when the championship is over. */
    public ChampionshipPhase currentPhase() {
        return phases.stream()
                .filter(state -> state.status() != PhaseStatus.FINISHED)
                .map(PhaseState::phase)
                .findFirst()
                .orElse(null);
    }

    public PhaseStatus statusOf(ChampionshipPhase phase) {
        return phases.stream().filter(state -> state.phase() == phase).map(PhaseState::status).findFirst().orElseThrow();
    }

    public List<MatchRef> matchesOf(ChampionshipPhase phase) {
        return matches.stream().filter(match -> match.stage().phase() == phase).toList();
    }

    // ------------------------------------------------------------------ tables

    public List<TableView> tables() {
        List<TableView> tables = new ArrayList<>();
        for (PhaseState state : phases) {
            if (!state.phase().hasPointsTable() || state.status() == PhaseStatus.PENDING) {
                continue;
            }
            tables.addAll(tablesOf(state.phase()));
        }
        return tables;
    }

    public List<TableView> tablesOf(ChampionshipPhase phase) {
        if (phase != ChampionshipPhase.GROUPS) {
            List<UUID> teamIds = teams.stream().map(TeamRef::id).toList();
            return List.of(new TableView(phase, null, StandingsCalculator.calculate(teamIds, matchesOf(phase), rules)));
        }
        return groupNumbers().stream()
                .map(group -> new TableView(phase, group, StandingsCalculator.calculate(teamIdsOfGroup(group), matchesOf(phase), rules)))
                .toList();
    }

    public List<Integer> groupNumbers() {
        return teams.stream().map(TeamRef::groupNumber).filter(Objects::nonNull).distinct().sorted().toList();
    }

    public boolean areGroupsDrawn() {
        return teams.stream().anyMatch(team -> team.groupNumber() != null);
    }

    public List<UUID> teamIdsOfGroup(int group) {
        return teams.stream().filter(team -> Objects.equals(team.groupNumber(), group)).map(TeamRef::id).toList();
    }

    // ------------------------------------------------------------------ rounds

    public List<RoundView> rounds() {
        List<RoundView> rounds = new ArrayList<>();
        for (ChampionshipPhase phase : rules.structure().phases()) {
            switch (phase) {
                case LEAGUE, SWISS -> rounds.addAll(matchdayRounds(phase));
                case GROUPS -> rounds.addAll(groupRounds());
                case KNOCKOUT -> rounds.addAll(knockoutRounds());
                case DOUBLE_ELIMINATION -> rounds.addAll(doubleEliminationRounds());
            }
        }
        return rounds;
    }

    private List<RoundView> matchdayRounds(ChampionshipPhase phase) {
        Map<Integer, List<MatchRef>> byRound = new TreeMap<>();
        for (MatchRef match : matchesOf(phase)) {
            byRound.computeIfAbsent(roundOf(match), key -> new ArrayList<>()).add(match);
        }
        return byRound.entrySet().stream()
                .map(entry -> new RoundView(phase, entry.getKey(), null, singleMatchViews(entry.getValue())))
                .toList();
    }

    private List<RoundView> groupRounds() {
        Map<List<Integer>, List<MatchRef>> byGroupAndRound = new TreeMap<>(Comparator
                .<List<Integer>>comparingInt(key -> key.get(1)).thenComparingInt(key -> key.get(0)));
        for (MatchRef match : matchesOf(ChampionshipPhase.GROUPS)) {
            byGroupAndRound.computeIfAbsent(List.of(match.groupNumber() == null ? 0 : match.groupNumber(), roundOf(match)),
                    key -> new ArrayList<>()).add(match);
        }
        return byGroupAndRound.entrySet().stream()
                .map(entry -> new RoundView(ChampionshipPhase.GROUPS, entry.getKey().get(1), entry.getKey().get(0),
                        singleMatchViews(entry.getValue())))
                .toList();
    }

    private static int roundOf(MatchRef match) {
        return match.roundNumber() == null ? LEAGUE_WITHOUT_MATCHDAY : match.roundNumber();
    }

    private List<TieView> singleMatchViews(List<MatchRef> roundMatches) {
        List<TieView> views = new ArrayList<>();
        int tieNumber = 1;
        for (MatchRef match : roundMatches) {
            views.add(new TieView(tieNumber++, match.stage(), match.homeTeamId(), match.awayTeamId(), false, List.of(match),
                    match.isDecided() ? match.homeScore() : 0, match.isDecided() ? match.awayScore() : 0, null, false));
        }
        return views;
    }

    private List<RoundView> knockoutRounds() {
        KnockoutState state = knockoutState();
        List<RoundView> rounds = new ArrayList<>();
        for (int round = 1; round <= Math.max(state.generatedRounds(), 1) && round <= state.totalRounds(); round++) {
            List<TieView> ties = new ArrayList<>(state.ties(round).stream().map(ChampionshipProgress::toView).toList());
            if (round == state.totalRounds()) {
                state.thirdPlace().filter(tie -> !tie.legs().isEmpty()).map(ChampionshipProgress::toView).ifPresent(ties::add);
            }
            if (!ties.isEmpty()) {
                rounds.add(new RoundView(ChampionshipPhase.KNOCKOUT, round, null, ties));
            }
        }
        return rounds;
    }

    private static TieView toView(KnockoutState.Tie tie) {
        return new TieView(tie.tieNumber(), tie.stage(), tie.teamA(), tie.teamB(), tie.bye(), tie.legs(),
                tie.outcome().teamAGoals(), tie.outcome().teamBGoals(), tie.outcome().winnerTeamId(),
                tie.outcome().status() == TieResolver.Status.NEEDS_TIEBREAK);
    }

    private List<RoundView> doubleEliminationRounds() {
        Map<Integer, List<TieView>> byRound = new TreeMap<>();
        for (DoubleEliminationState.Tie tie : doubleEliminationState().ties()) {
            byRound.computeIfAbsent(tie.round(), key -> new ArrayList<>()).add(new TieView(tie.tieNumber(), tie.stage(), tie.teamA(),
                    tie.teamB(), false, tie.legs(), tie.outcome().teamAGoals(), tie.outcome().teamBGoals(),
                    tie.outcome().winnerTeamId(), tie.outcome().status() == TieResolver.Status.NEEDS_TIEBREAK));
        }
        return byRound.entrySet().stream()
                .map(entry -> new RoundView(ChampionshipPhase.DOUBLE_ELIMINATION, entry.getKey(), null, entry.getValue()))
                .toList();
    }

    // ------------------------------------------------------------------ eliminations

    public KnockoutState knockoutState() {
        return KnockoutState.of(seedsOfTeams(), matchesOf(ChampionshipPhase.KNOCKOUT));
    }

    public DoubleEliminationState doubleEliminationState() {
        List<UUID> bySeed = teams.stream()
                .filter(team -> team.seed() != null)
                .sorted(Comparator.comparingInt(TeamRef::seed))
                .map(TeamRef::id)
                .toList();
        return new DoubleEliminationState(bySeed, rules.grandFinalReset(), matchesOf(ChampionshipPhase.DOUBLE_ELIMINATION));
    }

    private Map<UUID, Integer> seedsOfTeams() {
        return teams.stream()
                .filter(team -> team.seed() != null)
                .collect(Collectors.toMap(TeamRef::id, TeamRef::seed));
    }

    // ------------------------------------------------------------------ champion

    public Optional<UUID> champion() {
        ChampionshipPhase last = rules.structure().phases().get(rules.structure().phases().size() - 1);
        if (statusOf(last) != PhaseStatus.FINISHED) {
            return Optional.empty();
        }
        return switch (last) {
            case LEAGUE, SWISS, GROUPS -> firstOfTable(last);
            case KNOCKOUT -> knockoutState().champion();
            case DOUBLE_ELIMINATION -> doubleEliminationState().champion();
        };
    }

    /** The leader of the table, for a championship that ends on a points table (a group stage has no single leader). */
    private Optional<UUID> firstOfTable(ChampionshipPhase phase) {
        if (phase == ChampionshipPhase.GROUPS) {
            return Optional.empty();
        }
        return tablesOf(phase).get(0).rows().stream().findFirst().map(StandingRow::teamId);
    }

    // ------------------------------------------------------------------ phases

    private List<PhaseState> computePhases() {
        List<PhaseState> states = new ArrayList<>();
        boolean previousFinished = true;
        for (ChampionshipPhase phase : rules.structure().phases()) {
            List<MatchRef> phaseMatches = matchesOf(phase);
            PhaseStatus status = statusOf(phase, phaseMatches, previousFinished);
            states.add(new PhaseState(phase, status));
            previousFinished = status == PhaseStatus.FINISHED;
        }
        return states;
    }

    private PhaseStatus statusOf(ChampionshipPhase phase, List<MatchRef> phaseMatches, boolean previousFinished) {
        if (phaseMatches.isEmpty()) {
            return previousFinished ? PhaseStatus.READY : PhaseStatus.PENDING;
        }
        return isFinished(phase, phaseMatches) ? PhaseStatus.FINISHED : PhaseStatus.IN_PROGRESS;
    }

    private boolean isFinished(ChampionshipPhase phase, List<MatchRef> phaseMatches) {
        boolean allDecided = phaseMatches.stream().allMatch(MatchRef::isDecided);
        return switch (phase) {
            case LEAGUE, GROUPS -> allDecided;
            case SWISS -> allDecided && lastRoundOf(phaseMatches) >= rules.swissRounds();
            case KNOCKOUT -> isKnockoutFinished();
            case DOUBLE_ELIMINATION -> doubleEliminationState().champion().isPresent();
        };
    }

    private boolean isKnockoutFinished() {
        KnockoutState state = knockoutState();
        if (state.champion().isEmpty()) {
            return false;
        }
        boolean thirdPlaceStillOpen = rules.thirdPlaceMatch()
                && state.thirdPlace().map(tie -> !tie.isDecided()).orElse(false);
        return !thirdPlaceStillOpen;
    }

    private static int lastRoundOf(List<MatchRef> phaseMatches) {
        return phaseMatches.stream().mapToInt(ChampionshipProgress::roundOf).max().orElse(0);
    }

    public int lastRound(ChampionshipPhase phase) {
        return switch (phase) {
            case KNOCKOUT -> knockoutState().generatedRounds();
            case DOUBLE_ELIMINATION -> doubleEliminationState().lastRound();
            default -> lastRoundOf(matchesOf(phase));
        };
    }

    // ------------------------------------------------------------------ next step

    /** How many teams the format needs to start. */
    public static int requiredTeams(ChampionshipRules rules) {
        return switch (rules.structure()) {
            case ROUND_ROBIN, KNOCKOUT, DOUBLE_ELIMINATION -> 2;
            case SWISS -> Math.max(2, rules.swissRounds() + 1);
            case SWISS_KNOCKOUT -> Math.max(rules.swissRounds() + 1, Math.max(2, rules.playoffTeams()));
            case GROUPS_KNOCKOUT -> rules.groupCount() * Math.max(2, rules.teamsAdvancingPerGroup());
            case LEAGUE_KNOCKOUT -> Math.max(2, rules.playoffTeams());
        };
    }

    /** The Swiss system pairs everybody in every round, so it cannot have a team left over. */
    public static boolean requiresEvenTeams(ChampionshipRules rules) {
        return rules.structure() == EventFormatStructure.SWISS || rules.structure() == EventFormatStructure.SWISS_KNOCKOUT;
    }

    public NextStep nextStep() {
        int required = requiredTeams(rules);
        boolean evenRequired = requiresEvenTeams(rules);
        if (matches.isEmpty() && !hasEnoughTeams(required, evenRequired)) {
            return step(ChampionshipAction.NOT_ENOUGH_TEAMS, null, null, 0, List.of());
        }
        ChampionshipPhase current = currentPhase();
        if (current == null) {
            return step(ChampionshipAction.FINISHED, null, null, 0, List.of());
        }
        if (statusOf(current) == PhaseStatus.READY) {
            return step(isCalendarPhase(current) ? ChampionshipAction.GENERATE_CALENDAR : ChampionshipAction.GENERATE_ROUND,
                    current, 1, 0, List.of());
        }
        return nextStepOfRunningPhase(current);
    }

    private NextStep nextStepOfRunningPhase(ChampionshipPhase current) {
        long pending = matchesOf(current).stream().filter(match -> !match.isDecided()).count();
        if (pending > 0) {
            return step(ChampionshipAction.WAITING_RESULTS, current, null, (int) pending, List.of());
        }
        List<UUID> toResolve = matchesToResolve(current);
        if (!toResolve.isEmpty()) {
            return step(ChampionshipAction.NEEDS_TIEBREAK, current, null, 0, toResolve);
        }
        return step(ChampionshipAction.GENERATE_ROUND, current, lastRound(current) + 1, 0, List.of());
    }

    /** The deciding match of every tie that ended level. */
    private List<UUID> matchesToResolve(ChampionshipPhase phase) {
        return rounds().stream()
                .filter(round -> round.phase() == phase)
                .flatMap(round -> round.ties().stream())
                .filter(TieView::needsTiebreak)
                .map(tie -> TieResolver.decidingLeg(tie.legs()).id())
                .toList();
    }

    private boolean hasEnoughTeams(int required, boolean evenRequired) {
        return teams.size() >= required && (!evenRequired || teams.size() % 2 == 0);
    }

    /** The league and the group stage are generated whole; every other phase is generated round by round. */
    private static boolean isCalendarPhase(ChampionshipPhase phase) {
        return phase == ChampionshipPhase.LEAGUE || phase == ChampionshipPhase.GROUPS;
    }

    private NextStep step(ChampionshipAction action, ChampionshipPhase phase, Integer roundNumber, int pending, List<UUID> toResolve) {
        return new NextStep(action, phase, roundNumber, pending, teams.size(), requiredTeams(rules),
                requiresEvenTeams(rules), toResolve);
    }
}
