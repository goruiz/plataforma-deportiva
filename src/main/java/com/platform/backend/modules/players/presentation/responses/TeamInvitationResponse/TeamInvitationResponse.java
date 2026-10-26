package com.platform.backend.modules.players.presentation.responses.TeamInvitationResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeamInvitationResponse {

    private UUID id;
    private UUID teamId;
    /** Null if the team no longer exists. */
    private String teamName;
    private String type;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
