package com.platform.backend.modules.users.presentation.responses.RoleResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoleResponse {

    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
