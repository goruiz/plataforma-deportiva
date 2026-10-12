package com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTeamRequest {

    // Null means "leave unchanged"; a value, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 150)
    private String name;

    // Null leaves the logo as is; a blank text removes it.
    @Size(max = 500)
    private String logoUrl;

    private UUID categoryId;

    @Size(max = 20)
    private String status;
}
