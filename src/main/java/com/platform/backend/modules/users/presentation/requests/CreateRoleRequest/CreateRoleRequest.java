package com.platform.backend.modules.users.presentation.requests.CreateRoleRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank
    @Size(max = 255)
    private String name;
}
