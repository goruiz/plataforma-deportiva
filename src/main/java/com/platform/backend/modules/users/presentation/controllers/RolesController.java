package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IRolesService;
import com.platform.backend.modules.users.presentation.requests.CreateRoleRequest.CreateRoleRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateRoleRequest.UpdateRoleRequest;
import com.platform.backend.modules.users.presentation.responses.RoleResponse.RoleResponse;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolesController {

    private final IRolesService rolesService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(rolesService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(rolesService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolesService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(rolesService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        rolesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        rolesService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
