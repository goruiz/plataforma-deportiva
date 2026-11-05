package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.ForgotPasswordRequest.ForgotPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest.ResetPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;

public interface IUsersAuthService {

    /** Registers an account whose email still has to be confirmed (a link is emailed to it). */
    RegisterResponse register(RegisterRequest registerRequest);

    /** Registers an account whose email is already known to be its owner's, so it needs no confirmation. */
    RegisterResponse registerVerified(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    /** The signed-in account, as it is now (the token only has what it was when it was issued). */
    UserResponse getCurrentAccount(String email);

    /** A new app session for the account, with its current organization (see {@code PlayerAccountOrganizationService}). */
    AuthResponse reissueSession(String email);

    UpdateProfileResponse updateProfile(UpdateProfileRequest request, String email);

    void changePassword(ChangePasswordRequest request, String email);

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
