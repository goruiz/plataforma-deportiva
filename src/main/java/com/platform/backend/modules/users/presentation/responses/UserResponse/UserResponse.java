package com.platform.backend.modules.users.presentation.responses.UserResponse;

import lombok.Data;

import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String username;
    private String email;
    private UUID idRole;
}
