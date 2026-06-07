package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;

import java.util.List;
import java.util.UUID;

public interface IPlayersService {

    List<PlayerResponse> getAll();

    PlayerResponse getById(UUID id);

    PlayerResponse searchByEmail(String email);

    PlayerResponse create(CreatePlayerRequest request);

    PlayerResponse registerWithInvite(PlayerRegisterRequest request);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);

    void invitePlayer(InvitePlayerRequest request);

    void inviteRegister(InviteRegisterRequest request);

    List<PlayerResponse> getByTeamId(UUID teamId);

    void delete(UUID id);

    void hardDelete(UUID id);
}
