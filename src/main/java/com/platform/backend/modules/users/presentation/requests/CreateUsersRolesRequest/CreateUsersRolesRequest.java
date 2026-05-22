package com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateUsersRolesRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID roleId;
}
