package com.platform.backend.modules.courts.presentation.responses.CourtResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CourtResponse {

    private UUID id;
    private String name;
    private String description;
    private String location;
    private UUID sportComplexId;
    private String sportComplexName;
    /** The customer organization the court belongs to; null for the platform owner's own courts. */
    private UUID organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
