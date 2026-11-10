package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.enums.MatchEventType;
import com.platform.backend.modules.matches.domain.enums.MatchSide;
import com.platform.backend.modules.matches.domain.enums.TeamMatchResult;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The arithmetic of the team statistics, apart from where the numbers are read. */
final class TeamStatsCalculator {

    /** Goals first, because it is what a ranking is read for; then assists, fewer red and yellow cards, and the name. */
    static final Comparator<TeamPlayerStatsResponse> PLAYER_RANKING_ORDER =
            Comparator.comparingInt(TeamPlayerStatsResponse::getGoals).reversed()
                    .thenComparing(Comparator.comparingInt(TeamPlayerStatsResponse::getAssists).reversed())
                    .thenComparingInt(TeamPlayerStatsResponse::getRedCards)
                    .thenComparingInt(TeamPlayerStatsResponse::getYellowCards)
                    .thenComparing(TeamPlayerStatsResponse::getPlayerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private TeamStatsCalculator() {}

    /** What a group did in the timelines of the matches, by kind of event. */
    record EventTotals(int goals, int assists, int yellowCards, int redCards, int fouls) {

        static final EventTotals NONE = new EventTotals(0, 0, 0, 0, 0);

        EventTotals plus(MatchEventType type, int amount) {
            return switch (type) {
                case GOAL -> new EventTotals(goals + amount, assists, yellowCards, redCards, fouls);
                case ASSIST -> new EventTotals(goals, assists + amount, yellowCards, redCards, fouls);
                case YELLOW_CARD -> new EventTotals(goals, assists, yellowCards + amount, redCards, fouls);
                case RED_CARD -> new EventTotals(goals, assists, yellowCards, redCards + amount, fouls);
                case FOUL -> new EventTotals(goals, assists, yellowCards, redCards, fouls + amount);
            };
        }
    }

    /** Both sides of each team added together; a team that only played on one side keeps that side's numbers. */
    static Map<UUID, TeamMatchTally> mergeSides(List<TeamMatchTally> asHome, List<TeamMatchTally> asAway) {
        Map<UUID, TeamMatchTally> merged = new HashMap<>();
        asHome.forEach(tally -> merged.merge(tally.teamId(), tally, TeamMatchTally::plus));
        asAway.forEach(tally -> merged.merge(tally.teamId(), tally, TeamMatchTally::plus));
        return merged;
    }

    /** The counts of each group by type of event, folded into one line per group; an unknown type adds nothing. */
    static Map<UUID, EventTotals> sumEventsByGroup(List<GroupedCount> counts) {
        Map<UUID, EventTotals> totals = new HashMap<>();
        for (GroupedCount count : counts) {
            if (!MatchEventType.isValid(count.label())) {
                continue;
            }
            EventTotals totalsSoFar = totals.getOrDefault(count.groupId(), EventTotals.NONE);
            totals.put(count.groupId(), totalsSoFar.plus(MatchEventType.valueOf(count.label()), (int) count.count()));
        }
        return totals;
    }

    /** The match as the team lived it: which side it played, against whom, and how it ended (null without a result). */
    static TeamMatchHistoryResponse historyLine(MatchesEntity match, UUID teamId,
                                                Map<UUID, String> eventNames, Map<UUID, String> teamNames) {
        boolean atHome = teamId.equals(match.getHomeTeamId());
        UUID opponentId = atHome ? match.getAwayTeamId() : match.getHomeTeamId();

        TeamMatchHistoryResponse line = new TeamMatchHistoryResponse();
        line.setMatchId(match.getId());
        line.setMatchDate(match.getMatchDate());
        line.setEventId(match.getEventId());
        line.setEventName(match.getEventId() == null ? null : eventNames.get(match.getEventId()));
        line.setSide(atHome ? MatchSide.HOME : MatchSide.AWAY);
        line.setOpponentId(opponentId);
        line.setOpponentName(teamNames.get(opponentId));
        line.setStatus(match.getStatus());
        if (match.hasResult()) {
            int goalsFor = atHome ? match.getHomeScore() : match.getAwayScore();
            int goalsAgainst = atHome ? match.getAwayScore() : match.getHomeScore();
            line.setGoalsFor(goalsFor);
            line.setGoalsAgainst(goalsAgainst);
            line.setResult(TeamMatchResult.of(goalsFor, goalsAgainst));
        }
        return line;
    }
}
