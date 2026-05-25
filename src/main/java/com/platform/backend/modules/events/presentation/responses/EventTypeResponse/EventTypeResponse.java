package com.platform.backend.modules.events.presentation.responses.EventTypeResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventTypeResponse {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
