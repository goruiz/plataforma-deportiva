package com.platform.backend.modules.settings.presentation.controllers;

import com.platform.backend.modules.settings.application.iservices.ISystemSettingsService;
import com.platform.backend.modules.settings.presentation.requests.UpdateSettingsRequest.UpdateSettingsRequest;
import com.platform.backend.modules.settings.presentation.responses.SettingsResponse.SettingsResponse;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings", description = "The match rules of an organization (the platform owner edits the defaults)")
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final ISystemSettingsService settingsService;

    /** Open to any authenticated account: the mobile app reads the rules that apply to its player. */
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getForCaller()));
    }

    @PutMapping
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<SettingsResponse>> update(@Valid @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.update(request)));
    }
}
