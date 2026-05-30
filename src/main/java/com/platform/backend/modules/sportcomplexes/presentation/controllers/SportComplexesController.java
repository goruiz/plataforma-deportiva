package com.platform.backend.modules.sportcomplexes.presentation.controllers;

import com.platform.backend.modules.sportcomplexes.application.iservices.ISportComplexesService;
import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
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

@Tag(name = "Sport Complexes", description = "Sport complex management")
@RestController
@RequestMapping("/sport-complexes")
@RequiredArgsConstructor
public class SportComplexesController {

    private final ISportComplexesService sportComplexesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SportComplexResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(sportComplexesService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SportComplexResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sportComplexesService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SportComplexResponse>> create(@Valid @RequestBody CreateSportComplexRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(sportComplexesService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SportComplexResponse>> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateSportComplexRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sportComplexesService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        sportComplexesService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        sportComplexesService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
