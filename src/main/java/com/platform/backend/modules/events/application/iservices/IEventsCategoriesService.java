package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.presentation.requests.CreateEventsCategoriesRequest.CreateEventsCategoriesRequest;
import com.platform.backend.modules.events.presentation.responses.EventsCategoriesResponse.EventsCategoriesResponse;

import java.util.List;
import java.util.UUID;

public interface IEventsCategoriesService {

    List<EventsCategoriesResponse> getAll();

    EventsCategoriesResponse getById(UUID id);

    EventsCategoriesResponse create(CreateEventsCategoriesRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
