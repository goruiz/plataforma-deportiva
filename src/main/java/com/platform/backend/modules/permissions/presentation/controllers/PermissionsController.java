package com.platform.backend.modules.permissions.presentation.controllers;

import com.platform.backend.modules.permissions.application.iservices.IPermissionsService;
import com.platform.backend.modules.permissions.presentation.requests.CreatePermissionRequest.CreatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.requests.UpdatePermissionRequest.UpdatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.responses.PermissionResponse.PermissionResponse;
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
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionsController {

    private final IPermissionsService permissionsService;

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAll() {
        return ResponseEntity.ok(permissionsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(permissionsService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionsService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(permissionsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        permissionsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        permissionsService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
