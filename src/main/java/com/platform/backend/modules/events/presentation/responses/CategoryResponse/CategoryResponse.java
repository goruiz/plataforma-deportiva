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
    /** The gender the category admits ({@code MALE} or {@code FEMALE}); null admits both. */
    private String gender;
    /** The youngest age admitted, in years completed; null has no minimum. */
    private Integer minAge;
    /** The oldest age admitted, in years completed; null has no maximum. */
    private Integer maxAge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
