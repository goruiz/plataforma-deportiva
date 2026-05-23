package com.platform.backend.modules.menu.presentation.controllers;

import com.platform.backend.modules.menu.application.iservices.IRolesMenuService;
import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;
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

@Tag(name = "Roles - Menu", description = "Role-menu relationship management")
@RestController
@RequestMapping("/roles-menu")
@RequiredArgsConstructor
public class RolesMenuController {

    private final IRolesMenuService rolesMenuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RolesMenuResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(rolesMenuService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RolesMenuResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(rolesMenuService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RolesMenuResponse>> create(@Valid @RequestBody CreateRolesMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(rolesMenuService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RolesMenuResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateRolesMenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(rolesMenuService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        rolesMenuService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        rolesMenuService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
