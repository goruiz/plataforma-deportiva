package com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMatchEventRequest {

    @NotNull
    private UUID matchId;

    @NotNull
    private UUID playerId;

    @NotNull
    private UUID teamId;

    @NotBlank
    @Size(max = 30)
    private String eventType;

    private Integer minute;

    private String notes;
}
