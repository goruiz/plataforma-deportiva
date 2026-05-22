package com.platform.backend.modules.users.presentation.responses.UsersRolesResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UsersRolesResponse {

    private UUID id;
    private UUID userId;
    private UUID roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
