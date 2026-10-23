package com.platform.backend.modules.users.presentation.responses.RoleResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    /** The platform owner's role: the only one that can see (and always sees) the owner-only screens. */
    private boolean platformOwner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
