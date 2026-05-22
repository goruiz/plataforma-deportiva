package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchEventsService;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchEventRequest.CreateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchEventRequest.UpdateMatchEventRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchEventResponse.MatchEventResponse;
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

@RestController
@RequestMapping("/match-events")
@RequiredArgsConstructor
public class MatchEventsController {

    private final IMatchEventsService matchEventsService;

    @GetMapping
    public ResponseEntity<List<MatchEventResponse>> getAll() {
        return ResponseEntity.ok(matchEventsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchEventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(matchEventsService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MatchEventResponse> create(@Valid @RequestBody CreateMatchEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchEventsService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchEventResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateMatchEventRequest request) {
        return ResponseEntity.ok(matchEventsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        matchEventsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        matchEventsService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
