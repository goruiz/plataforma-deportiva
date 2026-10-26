package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventsService;
import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.exception.EventError;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventsService implements IEventsService {

    private static final String ID_EVENT_FORMAT_FIELD = "idEventFormat";

    private final IEventRepository eventRepository;
    private final IEventTypeRepository eventTypeRepository;
    private final IEventFormatRepository eventFormatRepository;
    private final IMatchRepository matchRepository;
    private final EventMapper eventMapper;

    /**
     * The event type given to an event created without one (the admin panel's events). The mobile app lists events by
     * type, so without it an event created in the panel would not show up there. Empty: they get none.
     */
    @Value("${app.events.default-event-type-id:}")
    private String defaultEventTypeId;

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
        EventsEntity event = eventMapper.toEntity(request);
        event.setEventType(resolveEventTypeForNewEvent(request.getIdEventType()));
        if (request.getIdEventFormat() != null) {
            event.setEventFormat(findActiveEventFormat(request.getIdEventFormat()));
        }
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    @Override
    public EventResponse update(UUID id, UpdateEventRequest request) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        CreatorAccess.requireAdminOrCreator(event.getCreatedBy());
        eventMapper.updateEntity(event, request);
        if (request.getIdEventType() != null) {
            EventTypesEntity eventType = eventTypeRepository.findActiveById(request.getIdEventType())
                    .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + request.getIdEventType()));
            event.setEventType(eventType);
        }
        if (request.getIdEventFormat() != null) {
            changeFormat(event, findActiveEventFormat(request.getIdEventFormat()));
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
        CreatorAccess.requireAdminOrCreator(event.getCreatedBy());
        SoftDeleteHelper.prepareForDeletion(event);
        eventRepository.delete(event);
    }

    @Override
    public void hardDelete(UUID id) {
        EventsEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        CreatorAccess.requireAdminOrCreator(event.getCreatedBy());
        eventRepository.hardDelete(event);
    }

    /** The type the client asked for; with none, the configured default one (or none if it is not configured). */
    private EventTypesEntity resolveEventTypeForNewEvent(UUID requestedTypeId) {
        if (requestedTypeId != null) {
            return eventTypeRepository.findActiveById(requestedTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + requestedTypeId));
        }
        if (!StringUtils.hasText(defaultEventTypeId)) {
            return null;
        }
        return eventTypeRepository.findActiveById(UUID.fromString(defaultEventTypeId)).orElse(null);
    }

    /** Matches generated for a phase only make sense for the format that generated them, so the format is then fixed. */
    private void changeFormat(EventsEntity event, EventFormatsEntity newFormat) {
        boolean isAnotherFormat = event.getEventFormat() == null || !event.getEventFormat().getId().equals(newFormat.getId());
        if (isAnotherFormat && hasChampionshipMatches(event.getId())) {
            throw new FieldValidationException(ID_EVENT_FORMAT_FIELD, EventError.FORMAT_LOCKED_BY_CHAMPIONSHIP.name());
        }
        event.setEventFormat(newFormat);
    }

    private boolean hasChampionshipMatches(UUID eventId) {
        return matchRepository.findAllActiveByEventId(eventId).stream().anyMatch(match -> match.getStage() != null);
    }

    private EventFormatsEntity findActiveEventFormat(UUID eventFormatId) {
        return eventFormatRepository.findActiveById(eventFormatId)
                .orElseThrow(() -> new EntityNotFoundException("Event format not found with id: " + eventFormatId));
    }
}
