package com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTeamRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String logoUrl;

    private UUID categoryId;

    @Size(max = 20)
    private String status;
}
