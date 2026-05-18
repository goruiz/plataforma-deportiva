package com.platform.backend.modules.players.presentation.responses.PlayerResponse;

import lombok.Data;

import java.time.LocalDateTime;
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
    private UUID teamId;
    private String teamName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
