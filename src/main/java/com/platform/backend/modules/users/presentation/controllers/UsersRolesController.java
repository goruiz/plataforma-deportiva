package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IUsersRolesService;
import com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest.CreateUsersRolesRequest;
import com.platform.backend.modules.users.presentation.responses.UsersRolesResponse.UsersRolesResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users - Roles", description = "User-role relationship management")
@RestController
@RequestMapping("/users-roles")
@RequiredArgsConstructor
public class UsersRolesController {

    private final IUsersRolesService usersRolesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsersRolesResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(usersRolesService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsersRolesResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(usersRolesService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsersRolesResponse>> create(@Valid @RequestBody CreateUsersRolesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(usersRolesService.create(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        usersRolesService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        usersRolesService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
