package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.ITeamStatsService;
import com.platform.backend.modules.matches.presentation.responses.TeamMatchHistoryResponse.TeamMatchHistoryResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse.TeamPlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.TeamStatsResponse.TeamStatsResponse;
import com.platform.backend.shared.infraestructure.pagination.ListQueryResolver;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.requests.ListRequest.ListRequest;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Lives with the matches because everything here is counted from the matches and the timelines of the team.
@Tag(name = "Team stats", description = "What a team has done: totals, matches and how its players did")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamStatsController {

    private final ITeamStatsService teamStatsService;
    private final ListQueryResolver listQueryResolver;

    /** The totals of every team the caller sees (the teams report); only administrators read them all at once. */
    @GetMapping("/stats")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<List<TeamStatsResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(teamStatsService.getAllTeamStats()));
    }

    @GetMapping("/{teamId}/stats")
    public ResponseEntity<ApiResponse<TeamStatsResponse>> getByTeamId(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamStatsService.getTeamStats(teamId)));
    }

    /** The matches of a team, the most recent first ({@code page} and {@code size}; {@code search} is not read). */
    @GetMapping("/{teamId}/matches")
    public ResponseEntity<ApiResponse<PageResponse<TeamMatchHistoryResponse>>> getMatches(@PathVariable UUID teamId,
                                                                                         ListRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(teamStatsService.getTeamMatches(
                teamId, listQueryResolver.pageQuery(request.getPage(), request.getSize())))));
    }

    @GetMapping("/{teamId}/player-stats")
    public ResponseEntity<ApiResponse<List<TeamPlayerStatsResponse>>> getPlayerStats(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamStatsService.getTeamPlayerStats(teamId)));
    }
}
