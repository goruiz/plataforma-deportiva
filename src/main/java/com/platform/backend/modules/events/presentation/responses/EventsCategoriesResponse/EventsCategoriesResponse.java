package com.platform.backend.modules.events.presentation.responses.EventsCategoriesResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventsCategoriesResponse {

    private UUID id;
    private UUID eventId;
    private UUID categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
