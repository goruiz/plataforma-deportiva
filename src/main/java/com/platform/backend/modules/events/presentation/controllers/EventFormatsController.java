package com.platform.backend.modules.events.presentation.controllers;

import com.platform.backend.modules.events.application.iservices.IEventFormatsService;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Event Formats", description = "Event format management")
@RestController
@RequestMapping("/event-formats")
@RequiredArgsConstructor
public class EventFormatsController {

    private final IEventFormatsService eventFormatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventFormatResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(eventFormatsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventFormatResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventFormatsService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventFormatResponse>> create(@Valid @RequestBody CreateEventFormatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(eventFormatsService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventFormatResponse>> update(@PathVariable UUID id,
                                                                   @Valid @RequestBody UpdateEventFormatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventFormatsService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        eventFormatsService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
