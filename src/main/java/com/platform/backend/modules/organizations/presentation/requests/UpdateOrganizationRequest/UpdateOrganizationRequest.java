package com.platform.backend.modules.organizations.presentation.requests.UpdateOrganizationRequest;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrganizationRequest {

    // Null means "leave unchanged"; a value, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 150)
    private String name;

    // Null leaves the description as is; a blank text removes it.
    @Size(max = 500)
    private String description;

    @Size(max = 20)
    private String status;
}
