package com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePlayerCaptainRequest {

    /** True makes the player the captain of their team (the previous captain steps down); false removes the captaincy. */
    @NotNull
    private Boolean isCaptain;
}
