package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A match of the event, as the championship sees it (what it needs from the stored match).
 * <p>
 * The penalty scores are those of the shoot-out that settled a result that ended level; they are null for every match
 * that had none.
 */
public record MatchRef(UUID id, UUID homeTeamId, UUID awayTeamId, Integer homeScore, Integer awayScore,
                       MatchStage stage, Integer roundNumber, Integer groupNumber, Integer tieNumber,
                       Integer legNumber, UUID advancingTeamId, LocalDateTime matchDate, String status,
                       Integer homePenaltyScore, Integer awayPenaltyScore) {

    /** A match without a shoot-out: what the championship knew before penalties existed. */
    public MatchRef(UUID id, UUID homeTeamId, UUID awayTeamId, Integer homeScore, Integer awayScore,
                    MatchStage stage, Integer roundNumber, Integer groupNumber, Integer tieNumber,
                    Integer legNumber, UUID advancingTeamId, LocalDateTime matchDate, String status) {
        this(id, homeTeamId, awayTeamId, homeScore, awayScore, stage, roundNumber, groupNumber, tieNumber,
                legNumber, advancingTeamId, matchDate, status, null, null);
    }

    /** A match counts once it has a result, whatever its status says. */
    public boolean isDecided() {
        return homeScore != null && awayScore != null;
    }

    public boolean involves(UUID teamId) {
        return homeTeamId.equals(teamId) || awayTeamId.equals(teamId);
    }

    /** The goals of {@code teamId} in this match (the match must be decided and the team must have played it). */
    public int goalsOf(UUID teamId) {
        return homeTeamId.equals(teamId) ? homeScore : awayScore;
    }

    /** The team that won the penalty shoot-out of this match; null when there was none or it did not decide. */
    public UUID penaltyWinnerId() {
        if (homePenaltyScore == null || awayPenaltyScore == null || homePenaltyScore.equals(awayPenaltyScore)) {
            return null;
        }
        return homePenaltyScore > awayPenaltyScore ? homeTeamId : awayTeamId;
    }
}
