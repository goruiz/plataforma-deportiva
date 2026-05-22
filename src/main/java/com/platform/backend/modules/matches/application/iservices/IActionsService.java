package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.CreateActionRequest.CreateActionRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateActionRequest.UpdateActionRequest;
import com.platform.backend.modules.matches.presentation.responses.ActionResponse.ActionResponse;

import java.util.List;
import java.util.UUID;

public interface IActionsService {

    List<ActionResponse> getAll();

    ActionResponse getById(UUID id);

    ActionResponse create(CreateActionRequest request);

    ActionResponse update(UUID id, UpdateActionRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
