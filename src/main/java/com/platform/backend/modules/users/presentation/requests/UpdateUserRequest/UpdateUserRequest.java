package com.platform.backend.modules.users.presentation.requests.UpdateUserRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String middleName;

    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String secondLastName;

    @Size(max = 255)
    private String username;

    @Email
    @Size(max = 255)
    private String email;
}
