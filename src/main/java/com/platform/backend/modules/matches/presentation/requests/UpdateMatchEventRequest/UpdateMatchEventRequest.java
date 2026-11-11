package com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest;

import jakarta.validation.constraints.PositiveOrZero;
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

    @PositiveOrZero
    private Integer minute;

    private String notes;

    /** Only for a {@code SUBSTITUTION}: the player who goes out ({@code playerId} is the one who comes in). */
    private UUID replacedPlayerId;
}
