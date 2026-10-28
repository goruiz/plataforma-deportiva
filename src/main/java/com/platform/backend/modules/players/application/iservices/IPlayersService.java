package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InvitePlayerRequest.InvitePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest.InviteRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.PlayerRegisterRequest.PlayerRegisterRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerCaptainRequest.UpdatePlayerCaptainRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerStatusRequest.UpdatePlayerStatusRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;

import java.util.List;
import java.util.UUID;

public interface IPlayersService {

    List<PlayerResponse> getAll();

    PlayerResponse getById(UUID id);

    PlayerResponse searchByEmail(String email);

    /** The player that goes with the account that is calling (matched by email). */
    PlayerResponse getCurrent();

    PlayerResponse create(CreatePlayerRequest request);

    PlayerResponse registerWithInvite(PlayerRegisterRequest request);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);

    void invitePlayer(InvitePlayerRequest request);

    void inviteRegister(InviteRegisterRequest request);

    List<PlayerResponse> getByTeamId(UUID teamId);

    PlayerResponse removeFromTeam(UUID id);

    PlayerResponse updateStatus(UUID id, UpdatePlayerStatusRequest request);

    /** Designates (or removes) the captain of the player's team; there is at most one per team. */
    PlayerResponse updateCaptain(UUID id, UpdatePlayerCaptainRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
