package com.platform.backend.modules.events.presentation.controllers;

import com.platform.backend.modules.events.application.iservices.IEventsCategoriesService;
import com.platform.backend.modules.events.presentation.requests.CreateEventsCategoriesRequest.CreateEventsCategoriesRequest;
import com.platform.backend.modules.events.presentation.responses.EventsCategoriesResponse.EventsCategoriesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events-categories")
@RequiredArgsConstructor
public class EventsCategoriesController {

    private final IEventsCategoriesService eventsCategoriesService;

    @GetMapping
    public ResponseEntity<List<EventsCategoriesResponse>> getAll() {
        return ResponseEntity.ok(eventsCategoriesService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventsCategoriesResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventsCategoriesService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EventsCategoriesResponse> create(@Valid @RequestBody CreateEventsCategoriesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventsCategoriesService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventsCategoriesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        eventsCategoriesService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
