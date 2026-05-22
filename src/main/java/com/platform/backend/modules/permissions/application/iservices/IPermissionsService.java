package com.platform.backend.modules.permissions.application.iservices;

import com.platform.backend.modules.permissions.presentation.requests.CreatePermissionRequest.CreatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.requests.UpdatePermissionRequest.UpdatePermissionRequest;
import com.platform.backend.modules.permissions.presentation.responses.PermissionResponse.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface IPermissionsService {

    List<PermissionResponse> getAll();

    PermissionResponse getById(UUID id);

    PermissionResponse create(CreatePermissionRequest request);

    PermissionResponse update(UUID id, UpdatePermissionRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
