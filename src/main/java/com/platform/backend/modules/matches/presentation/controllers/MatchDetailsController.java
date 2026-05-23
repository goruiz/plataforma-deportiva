package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchDetailsService;
import com.platform.backend.modules.matches.presentation.requests.CreateMatchDetailRequest.CreateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateMatchDetailRequest.UpdateMatchDetailRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchDetailResponse.MatchDetailResponse;
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

@Tag(name = "Match Details", description = "Match detail management")
@RestController
@RequestMapping("/match-details")
@RequiredArgsConstructor
public class MatchDetailsController {

    private final IMatchDetailsService matchDetailsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MatchDetailResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(matchDetailsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(matchDetailsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MatchDetailResponse>> create(@Valid @RequestBody CreateMatchDetailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(matchDetailsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> update(@PathVariable UUID id,
                                                                   @Valid @RequestBody UpdateMatchDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(matchDetailsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        matchDetailsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        matchDetailsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
