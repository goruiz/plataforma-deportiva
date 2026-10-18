package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;
import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.matches.domain.enums.ChampionshipAction;
import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Plays a whole championship against the engine: generates what it asks for, "plays" the matches, resolves the ties
 * that end level and goes on until it is over. The first team given is the strongest; by default a stronger team
 * beats a weaker one 2-0.
 */
final class ChampionshipSimulator {

    /** Decides a match: the goals of {@code [home, away]}. */
    interface ResultRule {
        int[] goals(MatchRef match, List<UUID> strongestFirst);
    }

    static final ResultRule STRONGER_WINS = (match, strongestFirst) ->
            strongestFirst.indexOf(match.homeTeamId()) < strongestFirst.indexOf(match.awayTeamId()) ? new int[]{2, 0} : new int[]{0, 2};

    static final ResultRule EVERYTHING_IS_A_DRAW = (match, strongestFirst) -> new int[]{1, 1};

    private static final int SAFETY_LIMIT = 200;

    private final ChampionshipRules rules;
    private final List<UUID> strongestFirst;
    private final ResultRule resultRule;
    private final List<MatchRef> matches = new ArrayList<>();
    private List<TeamRef> teams;

    ChampionshipSimulator(ChampionshipRules rules, int teamCount, ResultRule resultRule) {
        this.rules = rules;
        this.strongestFirst = EngineFixtures.teams(teamCount);
        this.resultRule = resultRule;
        this.teams = strongestFirst.stream().map(id -> new TeamRef(id, id.toString(), null, null)).toList();
    }

    static ChampionshipRules rules(EventFormatStructure structure, boolean roundTrip, int swissRounds, int groupCount, int advancing,
                                   int playoffTeams, boolean knockoutRoundTrip, boolean thirdPlace, boolean grandFinalReset) {
        return new ChampionshipRules(structure, roundTrip, 3, 1, 0,
                List.of(TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR),
                swissRounds, groupCount, advancing, playoffTeams, knockoutRoundTrip, thirdPlace, grandFinalReset);
    }

    List<UUID> teamIds() { return strongestFirst; }

    List<MatchRef> matches() { return matches; }

    ChampionshipProgress progress() {
        return ChampionshipProgress.of(rules, teams, matches);
    }

    ChampionshipProgress.NextStep nextStep() {
        return progress().nextStep();
    }

    /** Generates what the championship asks for next; the groups are drawn first when they are needed. */
    List<MatchRef> generate() {
        ChampionshipProgress progress = progress();
        if (progress.currentPhase() == ChampionshipPhase.GROUPS && !progress.areGroupsDrawn()) {
            drawGroups();
        }
        ChampionshipPlanner.Plan plan = ChampionshipPlanner.planNext(rules, teams, matches, UnaryOperator.identity());
        applySeeds(plan.seeds());
        List<MatchRef> created = plan.drafts().stream().map(this::toMatch).toList();
        matches.addAll(created);
        return created;
    }

    void drawGroups() {
        Map<UUID, Integer> groups = GroupDraw.assign(strongestFirst, rules.groupCount(), UnaryOperator.identity());
        teams = teams.stream().map(team -> new TeamRef(team.id(), team.name(), groups.get(team.id()), team.seed())).toList();
    }

    void playPendingMatches() {
        for (int index = 0; index < matches.size(); index++) {
            MatchRef match = matches.get(index);
            if (!match.isDecided()) {
                int[] goals = resultRule.goals(match, strongestFirst);
                matches.set(index, withResult(match, goals[0], goals[1], match.advancingTeamId()));
            }
        }
    }

    /** The stronger team advances in every tie that ended level. */
    void resolveTiebreaks() {
        for (UUID matchId : nextStep().matchesToResolve()) {
            int index = indexOf(matchId);
            MatchRef match = matches.get(index);
            UUID stronger = strongestFirst.indexOf(match.homeTeamId()) < strongestFirst.indexOf(match.awayTeamId())
                    ? match.homeTeamId() : match.awayTeamId();
            matches.set(index, withResult(match, match.homeScore(), match.awayScore(), stronger));
        }
    }

    /** Runs until the championship is over, and returns every action taken on the way. */
    List<ChampionshipAction> runToEnd() {
        List<ChampionshipAction> actions = new ArrayList<>();
        for (int step = 0; step < SAFETY_LIMIT; step++) {
            ChampionshipAction action = nextStep().action();
            actions.add(action);
            switch (action) {
                case GENERATE_CALENDAR, GENERATE_ROUND -> generate();
                case WAITING_RESULTS -> playPendingMatches();
                case NEEDS_TIEBREAK -> resolveTiebreaks();
                case FINISHED -> {
                    return actions;
                }
                default -> throw new AssertionError("The championship cannot go on: " + action);
            }
        }
        throw new AssertionError("The championship did not finish after " + SAFETY_LIMIT + " steps");
    }

    List<MatchRef> matchesOfStage(MatchStage stage) {
        return matches.stream().filter(match -> match.stage() == stage).toList();
    }

    private void applySeeds(Map<UUID, Integer> seeds) {
        if (!seeds.isEmpty()) {
            teams = teams.stream()
                    .map(team -> new TeamRef(team.id(), team.name(), team.groupNumber(), seeds.get(team.id())))
                    .toList();
        }
    }

    private MatchRef toMatch(MatchDraft draft) {
        return new MatchRef(UUID.randomUUID(), draft.homeTeamId(), draft.awayTeamId(), null, null, draft.stage(),
                draft.roundNumber(), draft.groupNumber(), draft.tieNumber(), draft.legNumber(), null, null, "SCHEDULED");
    }

    private MatchRef withResult(MatchRef match, int homeGoals, int awayGoals, UUID advancingTeamId) {
        return new MatchRef(match.id(), match.homeTeamId(), match.awayTeamId(), homeGoals, awayGoals, match.stage(),
                match.roundNumber(), match.groupNumber(), match.tieNumber(), match.legNumber(), advancingTeamId, match.matchDate(), "FINISHED");
    }

    private int indexOf(UUID matchId) {
        for (int index = 0; index < matches.size(); index++) {
            if (matches.get(index).id().equals(matchId)) {
                return index;
            }
        }
        throw new AssertionError("Unknown match " + matchId);
    }
}
