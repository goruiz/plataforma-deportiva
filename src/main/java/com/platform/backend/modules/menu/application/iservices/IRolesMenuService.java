package com.platform.backend.modules.menu.application.iservices;

import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;

import java.util.List;
import java.util.UUID;

public interface IRolesMenuService {

    List<RolesMenuResponse> getAll();

    RolesMenuResponse getById(UUID id);

    RolesMenuResponse create(CreateRolesMenuRequest request);

    RolesMenuResponse update(UUID id, UpdateRolesMenuRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
