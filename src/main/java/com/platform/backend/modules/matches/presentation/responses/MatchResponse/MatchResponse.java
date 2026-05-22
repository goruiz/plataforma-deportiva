package com.platform.backend.modules.matches.presentation.responses.MatchResponse;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
