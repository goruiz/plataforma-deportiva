package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;

import java.util.List;
import java.util.UUID;

public interface IMatchesService {

    List<MatchResponse> getAll();

    MatchResponse getById(UUID id);

    List<MatchResponse> getByEventId(UUID eventId);

    MatchResponse create(CreateMatchRequest request);

    MatchResponse update(UUID id, UpdateMatchRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
