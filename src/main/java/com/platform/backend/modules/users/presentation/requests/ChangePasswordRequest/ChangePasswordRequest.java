package com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest;

import com.platform.backend.shared.infraestructure.security.password.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
    private String newPassword;
}
