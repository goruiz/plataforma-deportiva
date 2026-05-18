package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IUsersService;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UsersManagementController {

    private final IUsersService usersService;

    public UsersManagementController(IUsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(usersService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(usersService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        usersService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        usersService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
