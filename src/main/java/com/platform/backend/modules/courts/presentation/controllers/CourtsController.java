package com.platform.backend.modules.courts.presentation.controllers;

import com.platform.backend.modules.courts.application.iservices.ICourtsService;
import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
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

@Tag(name = "Courts", description = "Court management")
@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtsController {

    private final ICourtsService courtsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(courtsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourtResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(courtsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourtResponse>> create(@Valid @RequestBody CreateCourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(courtsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourtResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateCourtRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(courtsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        courtsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        courtsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
