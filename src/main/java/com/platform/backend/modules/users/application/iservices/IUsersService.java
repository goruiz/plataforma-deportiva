package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.LoginRequest.LoginRequest;
import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateProfileRequest.UpdateProfileRequest;
import com.platform.backend.modules.users.presentation.responses.AuthResponse.AuthResponse;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import com.platform.backend.modules.users.presentation.responses.UpdateProfileResponse.UpdateProfileResponse;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;

import java.util.List;
import java.util.UUID;

public interface IUsersService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(UUID id);

    RegisterResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    UpdateProfileResponse updateProfile(UpdateProfileRequest request, String email);

    void softDelete(UUID id);

    void hardDelete(UUID id);
}
