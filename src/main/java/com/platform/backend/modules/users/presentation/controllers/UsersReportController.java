package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IUsersReportService;
import com.platform.backend.modules.users.presentation.responses.UsersReportResponse.UsersReportResponse;
import com.platform.backend.shared.infraestructure.security.AccessRules;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users report", description = "Accounts and players added up (administrators only)")
@RestController
@RequestMapping("/users")
@PreAuthorize(AccessRules.ADMIN_ONLY)
@RequiredArgsConstructor
public class UsersReportController {

    private final IUsersReportService usersReportService;

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<UsersReportResponse>> getReport() {
        return ResponseEntity.ok(ApiResponse.ok(usersReportService.getReport()));
    }
}
