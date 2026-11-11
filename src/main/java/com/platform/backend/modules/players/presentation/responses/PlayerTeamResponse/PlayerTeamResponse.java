package com.platform.backend.modules.players.presentation.responses.PlayerTeamResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** One team a player plays for. A player can play for several, one per organization. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTeamResponse {

    private UUID teamId;
    private String teamName;
    /** The team's organization; null for the platform owner's teams. */
    private UUID organizationId;
    private Boolean isCaptain;
    /** Whether the player manages this team (see {@code team_members.is_team_admin}). */
    private Boolean isTeamAdmin;
    /** False when the player does not meet what the team's category asks (gender, age): they may not play for it. */
    private Boolean eligibleForCategory = true;
    /** Why they do not ({@code CategoryEligibilityError}); null when they do. */
    private String categoryIneligibility;

    public PlayerTeamResponse(UUID teamId, String teamName, UUID organizationId, Boolean isCaptain, Boolean isTeamAdmin) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.organizationId = organizationId;
        this.isCaptain = isCaptain;
        this.isTeamAdmin = isTeamAdmin;
    }
}
