package com.platform.backend.modules.players.presentation.responses.PlayerResponse;

import com.platform.backend.modules.players.presentation.responses.PlayerTeamResponse.PlayerTeamResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PlayerResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profilePhotoUrl;
    private String status;
    /**
     * The team the caller sees the player in: the one of their organization for an administrator, the last one joined for
     * the player themselves. A player can play for several teams (one per organization): they are all in {@link #teams}.
     */
    private UUID teamId;
    private String teamName;
    /**
     * Only set (true) when the player plays for a team of another organization than the caller's: the caller is told
     * who the person is and that they have a team, but not which one (see {@code PlayersService#searchByEmail}).
     */
    private Boolean inTeamOfAnotherOrganization;
    /** Whether the player captains the team of {@link #teamId}. */
    private Boolean isCaptain;
    /** The teams the caller can see the player in: only those of their organization for an administrator, all for the player. */
    private List<PlayerTeamResponse> teams = List.of();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
