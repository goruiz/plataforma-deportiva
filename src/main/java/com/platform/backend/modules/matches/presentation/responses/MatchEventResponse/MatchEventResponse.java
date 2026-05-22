package com.platform.backend.modules.matches.presentation.responses.MatchEventResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MatchEventResponse {

    private UUID id;
    private UUID matchId;
    private UUID playerId;
    private UUID teamId;
    private String eventType;
    private Integer minute;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
