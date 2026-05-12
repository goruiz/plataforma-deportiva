package com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

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
}
