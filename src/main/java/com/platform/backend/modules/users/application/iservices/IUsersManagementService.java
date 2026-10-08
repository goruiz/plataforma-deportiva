package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.CreateUserRequest.CreateUserRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateUserRequest.UpdateUserRequest;
import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;

import java.util.List;
import java.util.UUID;

public interface IUsersManagementService {

    List<UserResponse> getAllUsers();

    List<UserResponse> getAdminUsers();

    List<UserResponse> getRegularUsers();

    UserResponse getUserById(UUID id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void softDelete(UUID id);

    void hardDelete(UUID id);
}
