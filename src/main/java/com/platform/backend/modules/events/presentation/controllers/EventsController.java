package com.platform.backend.modules.events.presentation.controllers;

import com.platform.backend.modules.events.application.iservices.IEventsService;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Events", description = "Event management")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventsController {

    private final IEventsService eventsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(eventsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventsService.getById(id)));
    }

    @GetMapping("/by-event-type/{eventTypeId}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getByEventTypeId(@PathVariable UUID eventTypeId) {
        return ResponseEntity.ok(ApiResponse.ok(eventsService.getByEventTypeId(eventTypeId)));
    }

    @GetMapping("/by-event-type/{eventTypeId}/by-user/{userId}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getByEventTypeIdAndCreatedBy(
            @PathVariable UUID eventTypeId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(eventsService.getByEventTypeIdAndCreatedBy(eventTypeId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(eventsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        eventsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        eventsService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
