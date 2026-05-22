package com.platform.backend.modules.courts.presentation.controllers;

import com.platform.backend.modules.courts.application.iservices.ICourtsService;
import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
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
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtsController {

    private final ICourtsService courtsService;

    @GetMapping
    public ResponseEntity<List<CourtResponse>> getAll() {
        return ResponseEntity.ok(courtsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(courtsService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CourtResponse> create(@Valid @RequestBody CreateCourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courtsService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourtResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateCourtRequest request) {
        return ResponseEntity.ok(courtsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        courtsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        courtsService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
