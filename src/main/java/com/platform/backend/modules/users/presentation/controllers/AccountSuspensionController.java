package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IAccountSuspensionService;
import com.platform.backend.modules.users.presentation.requests.UpdateAccountSuspensionRequest.UpdateAccountSuspensionRequest;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users", description = "Suspending and reactivating accounts")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AccountSuspensionController {

    private final IAccountSuspensionService accountSuspensionService;

    /** Reversible, unlike deleting: the account keeps everything it has but cannot sign in while suspended. */
    @PatchMapping("/{id}/suspension")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<UserResponse>> changeSuspension(@PathVariable UUID id,
                                                                       @Valid @RequestBody UpdateAccountSuspensionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(accountSuspensionService.changeSuspension(id, request.getSuspended())));
    }
}
