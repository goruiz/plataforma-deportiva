package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;

import java.util.List;
import java.util.UUID;

public interface IEventsService {

    List<EventResponse> getAll();

    EventResponse getById(UUID id);

    EventResponse create(CreateEventRequest request);

    EventResponse update(UUID id, UpdateEventRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
