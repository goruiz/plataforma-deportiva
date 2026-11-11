package com.platform.backend.modules.users.presentation.requests.RegisterRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    // Optional: the open registration only asks for credentials (the profile is completed afterwards), but whoever
    // registers with a team invitation already gives their name. Same limit as the columns of the player they become.
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    // Optional too: the app asks for them when the profile is completed, and the registration with a team invitation
    // gives them at once (the team's category may depend on them).

    /** {@code MALE} or {@code FEMALE}. */
    @Size(max = 10)
    private String gender;

    @Past
    private LocalDate birthDate;
}