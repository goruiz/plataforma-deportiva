package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventTypesService;
import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.application.mappers.EventTypeMapper;
import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventTypeRequest.CreateEventTypeRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest.UpdateEventTypeRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventTypesService implements IEventTypesService {

    private final IEventTypeRepository eventTypeRepository;
    private final IEventRepository eventRepository;
    private final EventTypeMapper eventTypeMapper;
    private final EventMapper eventMapper;

    @Override
    public List<EventTypeResponse> getAll() {
        return eventTypeRepository.findAllActive()
                .stream()
                .map(eventTypeMapper::toResponse)
                .toList();
    }

    @Override
    public EventTypeResponse getById(UUID id) {
        EventTypesEntity eventType = eventTypeRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + id));
        return eventTypeMapper.toResponse(eventType);
    }

    @Override
    public List<EventResponse> getEventsByType(UUID id) {
        eventTypeRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + id));
        return eventRepository.findAllActiveByEventTypeId(id)
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public EventTypeResponse create(CreateEventTypeRequest request) {
        if (eventTypeRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Event type name already exists");
        }
        EventTypesEntity eventType = eventTypeMapper.toEntity(request);
        return eventTypeMapper.toResponse(eventTypeRepository.save(eventType));
    }

    @Override
    public EventTypeResponse update(UUID id, UpdateEventTypeRequest request) {
        EventTypesEntity eventType = eventTypeRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + id));
        eventTypeMapper.updateEntity(eventType, request);
        return eventTypeMapper.toResponse(eventTypeRepository.save(eventType));
    }

    @Override
    public void delete(UUID id) {
        EventTypesEntity eventType = eventTypeRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + id));
        eventType.setDeletedAt(LocalDateTime.now());
        eventTypeRepository.delete(eventType);
    }

    @Override
    public void hardDelete(UUID id) {
        EventTypesEntity eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + id));
        eventTypeRepository.hardDelete(eventType);
    }
}

