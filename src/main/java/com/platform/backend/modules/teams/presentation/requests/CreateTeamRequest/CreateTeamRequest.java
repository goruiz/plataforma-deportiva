package com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTeamRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String logoUrl;

    private UUID categoryId;
}
