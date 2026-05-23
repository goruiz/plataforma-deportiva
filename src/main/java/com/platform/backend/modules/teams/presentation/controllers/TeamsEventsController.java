package com.platform.backend.modules.teams.presentation.controllers;

import com.platform.backend.modules.teams.application.iservices.ITeamsEventsService;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest.CreateTeamsEventsRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse.TeamsEventsResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teams-events")
@RequiredArgsConstructor
public class TeamsEventsController {

    private final ITeamsEventsService teamsEventsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamsEventsResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(teamsEventsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamsEventsResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(teamsEventsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamsEventsResponse>> create(@Valid @RequestBody CreateTeamsEventsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teamsEventsService.create(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        teamsEventsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        teamsEventsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
