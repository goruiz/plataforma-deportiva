package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Builds a points table: points first, then the format's tie-breakers in their order. Teams still tied after all of
 * them keep the order they were given in, so the table is the same every time.
 */
public final class StandingsCalculator {

    private StandingsCalculator() {}

    /**
     * @param teamIds the teams of the table, even those that have not played
     * @param matches only the decided matches between two of those teams count
     */
    public static List<StandingRow> calculate(Collection<UUID> teamIds, List<MatchRef> matches, ChampionshipRules rules) {
        Map<UUID, Stats> stats = new LinkedHashMap<>();
        teamIds.forEach(teamId -> stats.put(teamId, new Stats()));

        List<MatchRef> counted = matches.stream()
                .filter(match -> match.isDecided() && stats.containsKey(match.homeTeamId()) && stats.containsKey(match.awayTeamId()))
                .toList();
        counted.forEach(match -> addResult(stats, match, rules));

        Map<UUID, Integer> opponentsPoints = sumOpponentsPoints(stats, counted);
        Context context = new Context(stats, counted, opponentsPoints, rules);
        List<UUID> ordered = orderTeams(new ArrayList<>(stats.keySet()), context);

        List<StandingRow> table = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            UUID teamId = ordered.get(index);
            Stats teamStats = stats.get(teamId);
            table.add(new StandingRow(teamId, index + 1, teamStats.played, teamStats.won, teamStats.drawn, teamStats.lost,
                    teamStats.goalsFor, teamStats.goalsAgainst, teamStats.points, opponentsPoints.get(teamId)));
        }
        return table;
    }

    private static void addResult(Map<UUID, Stats> stats, MatchRef match, ChampionshipRules rules) {
        Stats home = stats.get(match.homeTeamId());
        Stats away = stats.get(match.awayTeamId());
        home.add(match.homeScore(), match.awayScore(), rules);
        away.add(match.awayScore(), match.homeScore(), rules);
    }

    /** The sum of the points of every opponent a team has faced (Buchholz). */
    private static Map<UUID, Integer> sumOpponentsPoints(Map<UUID, Stats> stats, List<MatchRef> counted) {
        Map<UUID, Integer> sums = new HashMap<>();
        stats.keySet().forEach(teamId -> sums.put(teamId, 0));
        for (MatchRef match : counted) {
            sums.merge(match.homeTeamId(), stats.get(match.awayTeamId()).points, Integer::sum);
            sums.merge(match.awayTeamId(), stats.get(match.homeTeamId()).points, Integer::sum);
        }
        return sums;
    }

    private static List<UUID> orderTeams(List<UUID> teams, Context context) {
        List<UUID> ordered = new ArrayList<>();
        for (List<UUID> samePoints : partitionDescending(teams, team -> context.stats.get(team).points)) {
            ordered.addAll(breakTies(samePoints, 0, context));
        }
        return ordered;
    }

    /** Splits tied teams with the criterion at {@code criterionIndex}, and the teams still tied with the next one. */
    private static List<UUID> breakTies(List<UUID> tied, int criterionIndex, Context context) {
        List<TiebreakerCriterion> criteria = context.rules.tiebreakers();
        if (tied.size() < 2 || criterionIndex >= criteria.size()) {
            return tied;
        }
        ToIntFunction<UUID> value = valueOf(criteria.get(criterionIndex), tied, context);
        List<UUID> result = new ArrayList<>();
        for (List<UUID> sameValue : partitionDescending(tied, value)) {
            result.addAll(breakTies(sameValue, criterionIndex + 1, context));
        }
        return result;
    }

    private static ToIntFunction<UUID> valueOf(TiebreakerCriterion criterion, List<UUID> tied, Context context) {
        return switch (criterion) {
            case GOAL_DIFFERENCE -> team -> context.stats.get(team).goalsFor - context.stats.get(team).goalsAgainst;
            case GOALS_FOR -> team -> context.stats.get(team).goalsFor;
            case WINS -> team -> context.stats.get(team).won;
            case OPPONENTS_POINTS -> context.opponentsPoints::get;
            case HEAD_TO_HEAD -> headToHeadPoints(tied, context)::get;
        };
    }

    /** The points each tied team earned only in the matches among the tied teams. */
    private static Map<UUID, Integer> headToHeadPoints(List<UUID> tied, Context context) {
        Map<UUID, Integer> points = new HashMap<>();
        tied.forEach(team -> points.put(team, 0));
        for (MatchRef match : context.matches) {
            if (points.containsKey(match.homeTeamId()) && points.containsKey(match.awayTeamId())) {
                points.merge(match.homeTeamId(), pointsFor(match.homeScore(), match.awayScore(), context.rules), Integer::sum);
                points.merge(match.awayTeamId(), pointsFor(match.awayScore(), match.homeScore(), context.rules), Integer::sum);
            }
        }
        return points;
    }

    /** Groups the teams by value, best value first; the teams of a group keep their relative order. */
    private static List<List<UUID>> partitionDescending(List<UUID> teams, ToIntFunction<UUID> value) {
        TreeMap<Integer, List<UUID>> byValue = new TreeMap<>(Comparator.reverseOrder());
        for (UUID team : teams) {
            byValue.computeIfAbsent(value.applyAsInt(team), key -> new ArrayList<>()).add(team);
        }
        return new ArrayList<>(byValue.values());
    }

    private static int pointsFor(int goalsFor, int goalsAgainst, ChampionshipRules rules) {
        if (goalsFor > goalsAgainst) {
            return rules.pointsForWin();
        }
        return goalsFor == goalsAgainst ? rules.pointsForDraw() : rules.pointsForLoss();
    }

    private record Context(Map<UUID, Stats> stats, List<MatchRef> matches, Map<UUID, Integer> opponentsPoints,
                           ChampionshipRules rules) {
    }

    private static final class Stats {
        private int played;
        private int won;
        private int drawn;
        private int lost;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

        void add(int scored, int conceded, ChampionshipRules rules) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            points += pointsFor(scored, conceded, rules);
            if (scored > conceded) {
                won++;
            } else if (scored == conceded) {
                drawn++;
            } else {
                lost++;
            }
        }
    }
}
