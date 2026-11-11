package com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PlayerRegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String profilePhotoUrl;

    private String inviteToken;

    // What the team's category may ask about the person: optional here, but a team whose category asks for them
    // refuses a registration without them.

    /** {@code MALE} or {@code FEMALE}. */
    @Size(max = 10)
    private String gender;

    @Past
    private LocalDate birthDate;
}
