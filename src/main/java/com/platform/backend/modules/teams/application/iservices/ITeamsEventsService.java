package com.platform.backend.modules.teams.application.iservices;

import com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest.CreateTeamsEventsRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse.TeamsEventsResponse;

import java.util.List;
import java.util.UUID;

public interface ITeamsEventsService {

    List<TeamsEventsResponse> getAll();

    TeamsEventsResponse getById(UUID id);

    TeamsEventsResponse create(CreateTeamsEventsRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
