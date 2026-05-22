package com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeamsEventsResponse {

    private UUID id;
    private UUID teamId;
    private UUID eventId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
