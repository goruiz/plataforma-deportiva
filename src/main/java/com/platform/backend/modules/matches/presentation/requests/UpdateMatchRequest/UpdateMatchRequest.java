package com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UpdateMatchRequest {

    private UUID homeTeamId;

    private UUID awayTeamId;

    private LocalDateTime matchDate;

    @Size(max = 300)
    private String location;

    @Size(max = 20)
    private String status;

    private Integer homeScore;

    private Integer awayScore;

    /** The penalty shoot-out of a result that ended level; both or none, and only if the settings allow penalties. */
    private Integer homePenaltyScore;

    private Integer awayPenaltyScore;

    private UUID eventId;

    private UUID courtId;
}
