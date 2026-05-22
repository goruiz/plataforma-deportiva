package com.platform.backend.modules.users.presentation.requests.UpdateRoleRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @Size(max = 255)
    private String name;
}
