package com.platform.backend.modules.teams.presentation.controllers;

import com.platform.backend.modules.teams.application.iservices.ITeamsService;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Teams", description = "Team management")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamsController {

    private final ITeamsService teamsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(teamsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(teamsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teamsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(teamsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        teamsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        teamsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
