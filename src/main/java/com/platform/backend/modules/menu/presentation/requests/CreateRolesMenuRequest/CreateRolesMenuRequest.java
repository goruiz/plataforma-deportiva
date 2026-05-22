package com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRolesMenuRequest {

    @NotNull
    private UUID roleId;

    @NotNull
    private UUID menuId;

    private Boolean visible;

    private Boolean canRead;

    private Boolean canCreat;

    private Boolean canUpdate;

    private Boolean canDelete;
}
