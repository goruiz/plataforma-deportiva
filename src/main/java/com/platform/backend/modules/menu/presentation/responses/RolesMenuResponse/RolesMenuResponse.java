package com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RolesMenuResponse {

    private UUID id;
    private UUID roleId;
    private UUID menuId;
    private Boolean visible;
    private Boolean canRead;
    private Boolean canCreat;
    private Boolean canUpdate;
    private Boolean canDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
