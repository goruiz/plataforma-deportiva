package com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest;

import com.platform.backend.shared.infraestructure.security.password.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Size(max = PasswordPolicy.RESET_TOKEN_MAX_LENGTH)
    private String token;

    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
    private String newPassword;
}
