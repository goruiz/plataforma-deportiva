package com.platform.backend.modules.matches.presentation.responses.MatchDetailResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MatchDetailResponse {

    private UUID id;
    private UUID matchId;
    private UUID idPlayer;
    private UUID idTeam;
    private UUID actionId;
    private Short minute;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
