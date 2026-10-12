package com.platform.backend.modules.events.presentation.responses.EventFormatResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventFormatResponse {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
