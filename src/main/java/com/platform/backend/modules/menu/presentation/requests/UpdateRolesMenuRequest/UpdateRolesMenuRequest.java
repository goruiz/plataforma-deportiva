package com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest;

import lombok.Data;

@Data
public class UpdateRolesMenuRequest {

    private Boolean visible;

    private Boolean canRead;

    private Boolean canCreat;

    private Boolean canUpdate;

    private Boolean canDelete;
}
