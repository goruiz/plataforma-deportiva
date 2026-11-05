package com.platform.backend.modules.matches.presentation.responses.MatchResponse;

import com.platform.backend.modules.matches.domain.enums.MatchStage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MatchResponse {

    private UUID id;
    private UUID homeTeamId;
    private UUID awayTeamId;
    private LocalDateTime matchDate;
    private String location;
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private UUID eventId;
    private UUID courtId;

    // Names of what the ids above point to, so a client can label a match without downloading every team and event.
    // Null when the caller does not see that team or event; set on what is read, not on what a write answers.
    private String homeTeamName;
    private String awayTeamName;
    private String eventName;
    /** When the match is over (kick-off + duration + break): from then on its result can be recorded. */
    private LocalDateTime endsAt;

    // Where the match sits in the championship; all null for a match created by hand (a league match).
    private MatchStage stage;
    private Integer roundNumber;
    private Integer groupNumber;
    private Integer tieNumber;
    private Integer legNumber;
    /** The team that advances from a tie that ended level, as an administrator decided. */
    private UUID advancingTeamId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
