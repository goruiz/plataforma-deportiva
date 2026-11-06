package com.platform.backend.modules.events.presentation.responses.CategoryResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CategoryResponse {

    private UUID id;
    private String name;
    private String description;
    /** The customer organization this category belongs to; null for the platform owner's own. */
    private UUID organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
