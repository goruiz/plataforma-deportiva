package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.application.iservices.IAccountLifecycleService;
import com.platform.backend.modules.users.application.iservices.IEmailVerificationService;
import com.platform.backend.modules.users.application.iservices.IUsersAuthService;
import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.DeleteAccountRequest.DeleteAccountRequest;
import com.platform.backend.modules.users.presentation.requests.ForgotPasswordRequest.ForgotPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest.ResetPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.requests.VerifyEmailRequest.VerifyEmailRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication: login, register, email confirmation, profile, password and account")
@RestController
@RequestMapping("/users/auth")
public class UsersAuthController {

    private final IUsersAuthService usersAuthService;
    private final IEmailVerificationService emailVerificationService;
    private final IAccountLifecycleService accountLifecycleService;

    public UsersAuthController(IUsersAuthService usersAuthService,
                               IEmailVerificationService emailVerificationService,
                               IAccountLifecycleService accountLifecycleService) {
        this.usersAuthService = usersAuthService;
        this.emailVerificationService = emailVerificationService;
        this.accountLifecycleService = accountLifecycleService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(ApiResponse.ok(usersAuthService.login(loginRequest)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(ApiResponse.ok(usersAuthService.register(registerRequest)));
    }

    /** Ends every session of the account: the token that calls it, and any other issued until now, stop working. */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails principal) {
        accountLifecycleService.signOut(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** The signed-in account as it is now, e.g. to learn that its email was confirmed since the token was issued. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentAccount(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(usersAuthService.getCurrentAccount(principal.getUsername())));
    }

    /** Public: the link in the email carries the proof, the person opening it may not be signed in. */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@AuthenticationPrincipal UserDetails principal) {
        emailVerificationService.resendVerificationLink(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        usersAuthService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        usersAuthService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<UpdateProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(usersAuthService.updateProfile(request, principal.getUsername())));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        usersAuthService.changePassword(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** POST and not DELETE: the password travels in the body and some clients and proxies drop the body of a DELETE. */
    @PostMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        accountLifecycleService.deleteAccount(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
