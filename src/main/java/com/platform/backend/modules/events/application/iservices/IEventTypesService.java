package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.presentation.requests.CreateEventTypeRequest.CreateEventTypeRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest.UpdateEventTypeRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;

import java.util.List;
import java.util.UUID;

public interface IEventTypesService {

    List<EventTypeResponse> getAll();

    EventTypeResponse getById(UUID id);

    List<EventResponse> getEventsByType(UUID id);

    EventTypeResponse create(CreateEventTypeRequest request);

    EventTypeResponse update(UUID id, UpdateEventTypeRequest request);

    void delete(UUID id);

    void hardDelete(UUID id);
}
