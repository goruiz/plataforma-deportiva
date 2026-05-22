package com.platform.backend.modules.teams.presentation.controllers;

import com.platform.backend.modules.teams.application.iservices.ITeamsEventsService;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamsEventsRequest.CreateTeamsEventsRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamsEventsResponse.TeamsEventsResponse;
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
    public ResponseEntity<List<TeamsEventsResponse>> getAll() {
        return ResponseEntity.ok(teamsEventsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamsEventsResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamsEventsService.getById(id));
    }

    @PostMapping
    public ResponseEntity<TeamsEventsResponse> create(@Valid @RequestBody CreateTeamsEventsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamsEventsService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamsEventsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        teamsEventsService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
