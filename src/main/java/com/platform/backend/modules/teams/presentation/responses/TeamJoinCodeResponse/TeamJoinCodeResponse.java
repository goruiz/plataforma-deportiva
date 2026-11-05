package com.platform.backend.modules.teams.presentation.responses.TeamJoinCodeResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/** The code of a team. Only an administrator (or whoever created the team) can ask for it. */
@Data
@AllArgsConstructor
public class TeamJoinCodeResponse {

    private UUID teamId;

    /** Null while the team admits no codes. */
    private String joinCode;
}
