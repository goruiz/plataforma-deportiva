package com.platform.backend.modules.matches.presentation.requests.SetAdvancingTeamRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SetAdvancingTeamRequest {

    /** The team that advances from a tie that ended level on goals. */
    @NotNull
    private UUID teamId;
}
