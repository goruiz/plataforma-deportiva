package com.platform.backend.modules.menu.application.iservices;

import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;

import java.util.List;
import java.util.UUID;

public interface IMenuService {

    List<MenuTreeResponse> getAll();

    MenuResponse getById(UUID id);

    MenuResponse create(CreateMenuRequest request);

    MenuResponse update(UUID id, UpdateMenuRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
