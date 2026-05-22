package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.CreateUsersRolesRequest.CreateUsersRolesRequest;
import com.platform.backend.modules.users.presentation.responses.UsersRolesResponse.UsersRolesResponse;

import java.util.List;
import java.util.UUID;

public interface IUsersRolesService {

    List<UsersRolesResponse> getAll();

    UsersRolesResponse getById(UUID id);

    UsersRolesResponse create(CreateUsersRolesRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
