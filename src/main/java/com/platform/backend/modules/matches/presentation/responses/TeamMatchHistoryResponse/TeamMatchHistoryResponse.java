package com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse;

import com.platform.backend.modules.matches.domain.enums.MatchSide;
import com.platform.backend.modules.matches.domain.enums.TeamMatchResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** One match of a team's history, told from the team's point of view. */
@Data
public class TeamMatchHistoryResponse {

    private UUID matchId;
    private LocalDateTime matchDate;
    private UUID eventId;
    /** Null when the event is not one the caller can read. */
    private String eventName;
    private MatchSide side;
    private UUID opponentId;
    /** Null when the opponent is not a team the caller can read. */
    private String opponentName;
    /** Null while the match has no result. */
    private Integer goalsFor;
    private Integer goalsAgainst;
    /** Null while the match has no result. */
    private TeamMatchResult result;
    /** The match status as stored (SCHEDULED, FINISHED, POSTPONED…). */
    private String status;
}
