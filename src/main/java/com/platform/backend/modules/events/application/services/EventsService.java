package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventsService;
import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventsService implements IEventsService {

    private final IEventRepository eventRepository;
    private final EventMapper eventMapper;

    @Override
    public List<EventResponse> getAll() {
        return eventRepository.findAllActive()
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public EventResponse getById(UUID id) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return eventMapper.toResponse(event);
    }

    @Override
    public EventResponse create(CreateEventRequest request) {
        if (eventRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Event name already exists");
        }
        EventsEntity event = eventMapper.toEntity(request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse update(UUID id, UpdateEventRequest request) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        eventMapper.updateEntity(event, request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public void delete(UUID id) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        event.setDeletedAt(LocalDateTime.now());
        eventRepository.delete(event);
    }

    @Override
    public void hardDelete(UUID id) {
        EventsEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        eventRepository.hardDelete(event);
    }
}
