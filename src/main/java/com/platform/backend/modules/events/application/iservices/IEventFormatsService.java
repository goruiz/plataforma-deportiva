package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;

import java.util.List;
import java.util.UUID;

public interface IEventFormatsService {

    List<EventFormatResponse> getAll();

    EventFormatResponse getById(UUID id);

    EventFormatResponse create(CreateEventFormatRequest request);

    EventFormatResponse update(UUID id, UpdateEventFormatRequest request);

    /** Soft delete; refused while any active event still uses the format. */
    void delete(UUID id);
}
