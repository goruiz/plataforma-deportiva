package com.platform.backend.modules.users.presentation.requests.CreateUserRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateUserRequest {

    @NotBlank
    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String middleName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String secondLastName;

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // Only the platform owner chooses the organization of the new account; an organization's administrator always
    // creates inside their own, whatever is sent here.
    private UUID organizationId;
}
