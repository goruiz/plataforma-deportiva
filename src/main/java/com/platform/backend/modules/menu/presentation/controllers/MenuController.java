package com.platform.backend.modules.menu.presentation.controllers;

import com.platform.backend.modules.menu.application.iservices.IMenuService;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
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
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService menuService;

    @GetMapping
    public ResponseEntity<List<MenuTreeResponse>> getAll() {
        return ResponseEntity.ok(menuService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        menuService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
