package com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String middleName;

    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String secondLastName;

    @Size(min = 3, max = 255)
    private String username;

    /** {@code MALE} or {@code FEMALE}; null leaves it as it was. */
    @Size(max = 10)
    private String gender;

    /** Null leaves it as it was. */
    @Past
    private LocalDate birthDate;
}
