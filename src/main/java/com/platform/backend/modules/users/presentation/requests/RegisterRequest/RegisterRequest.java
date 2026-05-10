package com.platform.backend.modules.users.presentation.requests.RegisterRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

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
    @Size(min = 3, max = 255)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;
}