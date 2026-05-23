package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IActionsService;
import com.platform.backend.modules.matches.presentation.requests.CreateActionRequest.CreateActionRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateActionRequest.UpdateActionRequest;
import com.platform.backend.modules.matches.presentation.responses.ActionResponse.ActionResponse;
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
@RequestMapping("/actions")
@RequiredArgsConstructor
public class ActionsController {

    private final IActionsService actionsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(actionsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(actionsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ActionResponse>> create(@Valid @RequestBody CreateActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(actionsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ActionResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(actionsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        actionsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        actionsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
