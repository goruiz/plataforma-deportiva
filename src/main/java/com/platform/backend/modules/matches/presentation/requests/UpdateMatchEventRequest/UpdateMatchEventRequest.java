package com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateMatchEventRequest {

    private UUID matchId;

    private UUID playerId;

    private UUID teamId;

    @Size(max = 30)
    private String eventType;

    private Integer minute;

    private String notes;
}
