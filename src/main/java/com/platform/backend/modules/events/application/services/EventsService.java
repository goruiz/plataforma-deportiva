package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventsService;
import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventsService implements IEventsService {

    private final IEventRepository eventRepository;
    private final IEventTypeRepository eventTypeRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    @Override
    public List<EventResponse> getAll() {
        return eventRepository.findAllActive()
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public EventResponse getById(UUID id) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return eventMapper.toResponse(event);
    }

    @Transactional
    @Override
    public EventResponse create(CreateEventRequest request) {
        if (request.getName() != null && eventRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Event name already exists");
        }
        EventTypesEntity eventType = eventTypeRepository.findActiveById(request.getIdEventType())
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + request.getIdEventType()));
        EventsEntity event = eventMapper.toEntity(request);
        event.setEventType(eventType);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    @Override
    public EventResponse update(UUID id, UpdateEventRequest request) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        eventMapper.updateEntity(event, request);
        if (request.getIdEventType() != null) {
            EventTypesEntity eventType = eventTypeRepository.findActiveById(request.getIdEventType())
                    .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + request.getIdEventType()));
            event.setEventType(eventType);
        }
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventResponse> getByEventTypeId(UUID eventTypeId) {
        eventTypeRepository.findActiveById(eventTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + eventTypeId));
        return eventRepository.findAllActiveByEventTypeId(eventTypeId)
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventResponse> getByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy) {
        eventTypeRepository.findActiveById(eventTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + eventTypeId));
        return eventRepository.findAllActiveByEventTypeIdAndCreatedBy(eventTypeId, createdBy)
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        SoftDeleteHelper.prepareForDeletion(event);
        eventRepository.delete(event);
    }

    @Override
    public void hardDelete(UUID id) {
        EventsEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        eventRepository.hardDelete(event);
    }
}
