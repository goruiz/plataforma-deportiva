package com.platform.backend.modules.matches.presentation.responses.MatchDisputeResponse;

import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** A dispute with the names a screen needs to show it (the match, its teams and its event) without more requests. */
@Data
public class MatchDisputeResponse {

    private UUID id;
    private UUID matchId;
    private DisputeStatus status;

    /** The team that disputes. */
    private UUID teamId;
    private String teamName;

    private String reason;
    private Integer proposedHomeScore;
    private Integer proposedAwayScore;

    private UUID openedById;
    /** Who raised it, as they were called when they did. */
    private String openedByName;
    private LocalDateTime createdAt;

    private String resolutionNote;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    // The match as it stands now. Null where the caller cannot see the match, a team or the event.
    private UUID eventId;
    private String eventName;
    private UUID homeTeamId;
    private UUID awayTeamId;
    private String homeTeamName;
    private String awayTeamName;
    private LocalDateTime matchDate;
    private Integer homeScore;
    private Integer awayScore;
    private Integer homePenaltyScore;
    private Integer awayPenaltyScore;
}
