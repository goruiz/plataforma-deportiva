package com.platform.backend.modules.permissions.presentation.requests.UpdatePermissionRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePermissionRequest {

    @Size(max = 255)
    private String name;

    private UUID roleId;
}
