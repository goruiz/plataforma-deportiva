package com.platform.backend.modules.menu.presentation.controllers;

import com.platform.backend.modules.menu.application.iservices.IMenuService;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
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

@Tag(name = "Menu", description = "Navigation menu management")
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuTreeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(menuService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        menuService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        menuService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
