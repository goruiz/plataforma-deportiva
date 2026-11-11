package com.platform.backend.modules.events.presentation.controllers;

import com.platform.backend.modules.events.application.iservices.ICategoriesService;
import com.platform.backend.modules.events.presentation.requests.CreateCategoryRequest.CreateCategoryRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateCategoryRequest.UpdateCategoryRequest;
import com.platform.backend.modules.events.presentation.responses.CategoryResponse.CategoryResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import com.platform.backend.shared.infraestructure.security.AccessRules;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = "Event category management")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final ICategoriesService categoriesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(categoriesService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoriesService.getById(id)));
    }

    // Only an administrator creates, changes or deletes these: the app only reads them (the organization filter
    // already keeps each administrator inside their own).
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(categoriesService.create(request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoriesService.update(id, request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoriesService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        categoriesService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
