package com.platform.backend.modules.players.presentation.requests.UpdatePlayerTeamAdminRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePlayerTeamAdminRequest {

    /** True makes the player an administrator of their team; false removes it. A team can have several. */
    @NotNull
    private Boolean isTeamAdmin;

    /**
     * The team the role is about. Optional when the player plays for a single team the caller sees (an
     * administrator of an organization always sees one); the platform owner sees them all and has to say which.
     */
    private UUID teamId;
}
