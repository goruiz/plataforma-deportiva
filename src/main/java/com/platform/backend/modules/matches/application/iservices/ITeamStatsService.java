package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamStatsResponse.TeamStatsResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.List;
import java.util.UUID;

public interface ITeamStatsService {

    /** The totals of one team; a 404 when it is not one the caller can see. */
    TeamStatsResponse getTeamStats(UUID teamId);

    /** The totals of every team the caller can see, by name: what the teams report is made of. */
    List<TeamStatsResponse> getAllTeamStats();

    /** The matches of one team, the most recent first, told from the team's side. */
    PageResult<TeamMatchHistoryResponse> getTeamMatches(UUID teamId, PageQuery pageQuery);

    /** The players of one team ranked by what they did with it, the current roster included even with zeros. */
    List<TeamPlayerStatsResponse> getTeamPlayerStats(UUID teamId);
}
