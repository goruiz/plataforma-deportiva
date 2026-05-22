package com.platform.backend.modules.matches.presentation.requests.CreateMatchDetailRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMatchDetailRequest {

    @NotNull
    private UUID matchId;

    private UUID idPlayer;

    private UUID idTeam;

    private UUID actionId;

    private Short minute;
}
