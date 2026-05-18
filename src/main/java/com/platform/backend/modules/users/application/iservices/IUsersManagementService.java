package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.responses.UserResponse.UserResponse;

import java.util.List;
import java.util.UUID;

public interface IUsersManagementService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(UUID id);

    void softDelete(UUID id);

    void hardDelete(UUID id);
}
