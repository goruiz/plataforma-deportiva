package com.platform.backend.modules.menu.application.iservices;

import com.platform.backend.modules.menu.presentation.requests.CreateRolesMenuRequest.CreateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.SaveRoleMenusRequest.SaveRoleMenusRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateRolesMenuRequest.UpdateRolesMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.RolesMenuResponse.RolesMenuResponse;

import java.util.List;
import java.util.UUID;

public interface IRolesMenuService {

    List<RolesMenuResponse> getAll();

    RolesMenuResponse getById(UUID id);

    RolesMenuResponse create(CreateRolesMenuRequest request);

    RolesMenuResponse update(UUID id, UpdateRolesMenuRequest request);

    /**
     * Sets, in one step, which menu items a role sees: the listed ones become visible and every other one stops being
     * visible to it. Owner-only items are the owner role's: they are refused for any other role and always kept for it.
     * Answers with every assignment the role has afterwards.
     */
    List<RolesMenuResponse> saveForRole(UUID roleId, SaveRoleMenusRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
