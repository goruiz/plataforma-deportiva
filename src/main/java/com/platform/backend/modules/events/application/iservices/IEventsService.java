package com.platform.backend.modules.events.application.iservices;

import com.platform.backend.modules.events.domain.filters.EventListFilter;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.List;
import java.util.UUID;

public interface IEventsService {

    List<EventResponse> getAll();

    /** One page of the events the caller sees (their organization's and the public ones), most relevant first. */
    PageResult<EventResponse> search(EventListFilter filter, PageQuery pageQuery);

    EventResponse getById(UUID id);

    EventResponse create(CreateEventRequest request);

    EventResponse update(UUID id, UpdateEventRequest request);

    List<EventResponse> getByEventTypeId(UUID eventTypeId);

    List<EventResponse> getByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy);

    void delete(UUID id);

    void hardDelete(UUID id);
}
