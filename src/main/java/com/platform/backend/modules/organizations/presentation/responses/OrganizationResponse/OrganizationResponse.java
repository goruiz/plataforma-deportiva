package com.platform.backend.modules.organizations.presentation.responses.OrganizationResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
