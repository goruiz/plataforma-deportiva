package com.platform.backend.modules.teams.application.iservices;

import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;

import java.util.List;
import java.util.UUID;

public interface ITeamsService {

    List<TeamResponse> getAll();

    TeamResponse getById(UUID id);

    TeamResponse create(CreateTeamRequest request);

    TeamResponse update(UUID id, UpdateTeamRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
