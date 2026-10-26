package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.ChangePasswordRequest.ChangePasswordRequest;
import com.platform.backend.modules.users.presentation.requests.ForgotPasswordRequest.ForgotPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.ResetPasswordRequest.ResetPasswordRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;

public interface IUsersAuthService {

    RegisterResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    UpdateProfileResponse updateProfile(UpdateProfileRequest request, String email);

    void changePassword(ChangePasswordRequest request, String email);

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
