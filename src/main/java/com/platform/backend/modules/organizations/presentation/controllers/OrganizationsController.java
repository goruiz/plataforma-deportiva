package com.platform.backend.modules.organizations.presentation.controllers;

import com.platform.backend.modules.organizations.application.iservices.IOrganizationsService;
import com.platform.backend.modules.organizations.presentation.requests.CreateOrganizationRequest.CreateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.requests.UpdateOrganizationRequest.UpdateOrganizationRequest;
import com.platform.backend.modules.organizations.presentation.responses.OrganizationResponse.OrganizationResponse;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Organizations", description = "Customers of the platform (platform owner only)")
@RestController
@RequestMapping("/organizations")
@PreAuthorize(AccessRules.PLATFORM_OWNER_ONLY)
@RequiredArgsConstructor
public class OrganizationsController {

    private final IOrganizationsService organizationsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(organizationsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(organizationsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(organizationsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        organizationsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
