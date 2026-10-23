package com.platform.backend.modules.users.presentation.requests.UpdateUserRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

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

    // Only the platform owner moves an account to another organization; null leaves it where it is (so an account can't
    // be taken back to "no organization" from here), and for an organization's administrator it is ignored.
    private UUID organizationId;
}
