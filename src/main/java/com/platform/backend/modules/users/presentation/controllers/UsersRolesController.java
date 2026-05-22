package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IUsersRolesService;
import com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest.CreateUsersRolesRequest;
import com.platform.backend.modules.users.presentation.responses.UsersRolesResponse.UsersRolesResponse;
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
@RequestMapping("/users-roles")
@RequiredArgsConstructor
public class UsersRolesController {

    private final IUsersRolesService usersRolesService;

    @GetMapping
    public ResponseEntity<List<UsersRolesResponse>> getAll() {
        return ResponseEntity.ok(usersRolesService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsersRolesResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(usersRolesService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UsersRolesResponse> create(@Valid @RequestBody CreateUsersRolesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usersRolesService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        usersRolesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        usersRolesService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
