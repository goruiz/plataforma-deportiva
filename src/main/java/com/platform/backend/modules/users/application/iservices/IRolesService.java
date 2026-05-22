package com.platform.backend.modules.users.application.iservices;

import com.platform.backend.modules.users.presentation.requests.CreateRoleRequest.CreateRoleRequest;
import com.platform.backend.modules.users.presentation.requests.UpdateRoleRequest.UpdateRoleRequest;
import com.platform.backend.modules.users.presentation.responses.RoleResponse.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface IRolesService {

    List<RoleResponse> getAll();

    RoleResponse getById(UUID id);

    RoleResponse create(CreateRoleRequest request);

    RoleResponse update(UUID id, UpdateRoleRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
