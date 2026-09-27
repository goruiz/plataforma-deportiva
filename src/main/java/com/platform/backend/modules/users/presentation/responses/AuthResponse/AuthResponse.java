package com.platform.backend.modules.users.presentation.responses.AuthResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private boolean admin;
    private UUID id;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
}
