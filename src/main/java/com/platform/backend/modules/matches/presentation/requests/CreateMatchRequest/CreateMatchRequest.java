package com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateMatchRequest {

    @NotNull
    private UUID homeTeamId;

    @NotNull
    private UUID awayTeamId;

    @NotNull
    private LocalDateTime matchDate;

    @Size(max = 300)
    private String location;

    private UUID eventId;
}
