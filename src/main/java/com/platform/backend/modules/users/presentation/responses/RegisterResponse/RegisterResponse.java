package com.platform.backend.modules.users.presentation.responses.RegisterResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String token;
    /** Always false for an open registration (the confirmation link was just emailed); true when an invitation vouched for the email. */
    private boolean emailVerified;
}