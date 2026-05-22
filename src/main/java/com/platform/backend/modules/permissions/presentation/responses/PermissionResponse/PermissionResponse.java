package com.platform.backend.modules.permissions.presentation.responses.PermissionResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PermissionResponse {

    private UUID id;
    private String name;
    private UUID roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
