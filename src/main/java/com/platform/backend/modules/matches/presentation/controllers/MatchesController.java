package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchRequest.CreateMatchRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchRequest.UpdateMatchRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
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
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchesController {

    private final IMatchesService matchesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(matchesService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(matchesService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MatchResponse>> create(@Valid @RequestBody CreateMatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(matchesService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateMatchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(matchesService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        matchesService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        matchesService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
