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

    private UUID eventId;
}
