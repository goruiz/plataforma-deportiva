package com.platform.backend.modules.events.presentation.controllers;

import com.platform.backend.modules.events.application.iservices.IEventTypesService;
import com.platform.backend.modules.events.presentation.requests.CreateEventTypeRequest.CreateEventTypeRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest.UpdateEventTypeRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Event Types", description = "Event type management")
@RestController
@RequestMapping("/event-types")
@RequiredArgsConstructor
public class EventTypesController {

    private final IEventTypesService eventTypesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(eventTypesService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventTypeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventTypesService.getById(id)));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByType(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventTypesService.getEventsByType(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventTypeResponse>> create(@Valid @RequestBody CreateEventTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(eventTypesService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventTypeResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateEventTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventTypesService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        eventTypesService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        eventTypesService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
