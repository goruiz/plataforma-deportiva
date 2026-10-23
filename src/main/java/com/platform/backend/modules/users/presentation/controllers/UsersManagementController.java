package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IUsersManagementService;
import com.platform.backend.modules.users.domain.enums.UserSection;
import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserCountsResponse.UserCountsResponse;
import com.platform.backend.modules.users.presentation.responses.UserListItemResponse.UserListItemResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.pagination.ListQueryResolver;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.requests.ListRequest.ListRequest;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import com.platform.backend.shared.presentation.responses.PageResponse.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.UUID;

@Tag(name = "Users", description = "User management")
@RestController
@RequestMapping("/users")
public class UsersManagementController {

    private final IUsersManagementService usersManagementService;
    private final ListQueryResolver listQueryResolver;

    public UsersManagementController(IUsersManagementService usersManagementService, ListQueryResolver listQueryResolver) {
        this.usersManagementService = usersManagementService;
        this.listQueryResolver = listQueryResolver;
    }

    @GetMapping
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<PageResponse<UserListItemResponse>>> getAllUsers(ListRequest request) {
        return listUsers(UserSection.ALL, request);
    }

    /** Accounts of the platform owner: only the owner sees them (an organization's administrator has none of these). */
    @GetMapping("/owners")
    @PreAuthorize(AccessRules.PLATFORM_OWNER_ONLY)
    public ResponseEntity<ApiResponse<PageResponse<UserListItemResponse>>> getOwnerUsers(ListRequest request) {
        return listUsers(UserSection.OWNERS, request);
    }

    @GetMapping("/admins")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<PageResponse<UserListItemResponse>>> getAdminUsers(ListRequest request) {
        return listUsers(UserSection.ADMINS, request);
    }

    @GetMapping("/regular")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<PageResponse<UserListItemResponse>>> getRegularUsers(ListRequest request) {
        return listUsers(UserSection.APP_USERS, request);
    }

    /** The number on each tab of the users screen; only {@code search} is read from the request. */
    @GetMapping("/counts")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<UserCountsResponse>> countUsersBySection(ListRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                usersManagementService.countUsersBySection(listQueryResolver.searchTerms(request.getSearch()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(usersManagementService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(usersManagementService.createUser(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usersManagementService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id) {
        usersManagementService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        usersManagementService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    private ResponseEntity<ApiResponse<PageResponse<UserListItemResponse>>> listUsers(UserSection section, ListRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usersManagementService.getUsers(
                section,
                listQueryResolver.pageQuery(request.getPage(), request.getSize()),
                listQueryResolver.searchTerms(request.getSearch()))));
    }
}
