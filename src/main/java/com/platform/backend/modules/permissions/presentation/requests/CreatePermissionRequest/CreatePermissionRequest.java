package com.platform.backend.modules.permissions.presentation.requests.CreatePermissionRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePermissionRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private UUID roleId;
}
