package com.platform.backend.modules.teams.presentation.responses.TeamResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeamResponse {

    private UUID id;
    private String name;
    private String logoUrl;
    private UUID categoryId;
    private String status;
    /** The customer organization this team belongs to; null for the platform owner's own. */
    private UUID organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
