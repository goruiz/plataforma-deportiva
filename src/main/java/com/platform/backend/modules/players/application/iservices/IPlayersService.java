package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.presentation.requests.CreatePlayerRequest.CreatePlayerRequest;
import com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest.UpdatePlayerRequest;
import com.platform.backend.modules.players.presentation.responses.PlayerResponse.PlayerResponse;

import java.util.List;
import java.util.UUID;

public interface IPlayersService {

    List<PlayerResponse> getAll();

    PlayerResponse getById(UUID id);

    PlayerResponse create(CreatePlayerRequest request);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
