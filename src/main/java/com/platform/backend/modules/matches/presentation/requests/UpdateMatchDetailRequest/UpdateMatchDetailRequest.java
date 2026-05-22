package com.platform.backend.modules.matches.presentation.requests.UpdateMatchDetailRequest;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateMatchDetailRequest {

    private UUID matchId;

    private UUID idPlayer;

    private UUID idTeam;

    private UUID actionId;

    private Short minute;
}
