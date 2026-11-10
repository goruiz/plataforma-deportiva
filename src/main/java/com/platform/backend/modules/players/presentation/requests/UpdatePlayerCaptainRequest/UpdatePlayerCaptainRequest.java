package com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePlayerCaptainRequest {

    /** True makes the player the captain of their team (the previous captain steps down); false removes the captaincy. */
    @NotNull
    private Boolean isCaptain;

    /**
     * The team the captaincy is about. Optional when the player plays for a single team the caller sees (an
     * administrator of an organization always sees one); the platform owner sees them all and has to say which.
     */
    private UUID teamId;
}
