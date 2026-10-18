package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.time.LocalDateTime;
import java.util.UUID;

/** A match of the event, as the championship sees it (what it needs from the stored match). */
public record MatchRef(UUID id, UUID homeTeamId, UUID awayTeamId, Integer homeScore, Integer awayScore,
                       MatchStage stage, Integer roundNumber, Integer groupNumber, Integer tieNumber,
                       Integer legNumber, UUID advancingTeamId, LocalDateTime matchDate, String status) {

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
}
