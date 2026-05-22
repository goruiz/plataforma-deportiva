package com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTeamsEventsRequest {

    @NotNull
    private UUID teamId;

    @NotNull
    private UUID eventId;
}
