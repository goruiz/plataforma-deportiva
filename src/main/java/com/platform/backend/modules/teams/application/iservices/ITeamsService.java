package com.platform.backend.modules.teams.application.iservices;

import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamJoinCodeResponse.TeamJoinCodeResponse;
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

    /** The code players type to join the team; an administrator (or the creator of the team) reads it. */
    TeamJoinCodeResponse getJoinCode(UUID id);

    /** Gives the team a new code (the previous one stops working). */
    TeamJoinCodeResponse generateJoinCode(UUID id);

    /** Takes the code away: nobody can join with it any more. */
    TeamJoinCodeResponse disableJoinCode(UUID id);
}
