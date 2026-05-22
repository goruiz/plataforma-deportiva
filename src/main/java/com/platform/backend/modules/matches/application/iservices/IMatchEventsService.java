package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest.CreateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest.UpdateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchEventResponse.MatchEventResponse;

import java.util.List;
import java.util.UUID;

public interface IMatchEventsService {

    List<MatchEventResponse> getAll();

    MatchEventResponse getById(UUID id);

    MatchEventResponse create(CreateMatchEventRequest request);

    MatchEventResponse update(UUID id, UpdateMatchEventRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
