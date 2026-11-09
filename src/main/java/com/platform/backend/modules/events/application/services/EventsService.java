package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventsService;
import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;
import com.platform.backend.modules.events.domain.enums.EventStatus;
import com.platform.backend.modules.events.domain.filters.EventListFilter;
import com.platform.backend.modules.events.domain.exception.EventError;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsSportComplexesRepository;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventsService implements IEventsService {

    private static final String ID_EVENT_FORMAT_FIELD = "idEventFormat";
    private static final String STATUS_FIELD = "status";
    private static final String SPORT_COMPLEX_IDS_FIELD = "sportComplexIds";

    private final IEventRepository eventRepository;
    private final IEventTypeRepository eventTypeRepository;
    private final IEventFormatRepository eventFormatRepository;
    private final IMatchRepository matchRepository;
    private final IEventsSportComplexesRepository eventsSportComplexesRepository;
    private final ISportComplexRepository sportComplexRepository;
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
        return toResponses(eventRepository.findAllActive());
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<EventResponse> search(EventListFilter filter, PageQuery pageQuery) {
        if (filter.status() != null && !EventStatus.isValid(filter.status())) {
            throw new FieldValidationException(STATUS_FIELD, EventError.INVALID_STATUS.name());
        }
        PageResult<EventResponse> page = eventRepository.findActivePage(filter, pageQuery).map(eventMapper::toResponse);
        attachSportComplexIds(page.content());
        return page;
    }

    @Transactional(readOnly = true)
    @Override
    public EventResponse getById(UUID id) {
        EventsEntity event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return toResponse(event);
    }

    @Transactional
    @Override
    public EventResponse create(CreateEventRequest request) {
        if (request.getName() != null && eventRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Event name already exists");
        }
        Set<UUID> sportComplexIds = distinctIds(request.getSportComplexIds());
        requireActiveSportComplexes(sportComplexIds);
        EventsEntity event = eventMapper.toEntity(request);
        event.setEventType(resolveEventTypeForNewEvent(request.getIdEventType()));
        if (request.getIdEventFormat() != null) {
            event.setEventFormat(findActiveEventFormat(request.getIdEventFormat()));
        }
        EventsEntity saved = eventRepository.save(event);
        replaceSportComplexes(saved.getId(), sportComplexIds);
        return toResponse(saved);
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
        if (request.getSportComplexIds() != null) {
            Set<UUID> sportComplexIds = distinctIds(request.getSportComplexIds());
            requireActiveSportComplexes(sportComplexIds);
            replaceSportComplexes(event.getId(), sportComplexIds);
        }
        return toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventResponse> getByEventTypeId(UUID eventTypeId) {
        eventTypeRepository.findActiveById(eventTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + eventTypeId));
        return toResponses(eventRepository.findAllActiveByEventTypeId(eventTypeId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventResponse> getByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy) {
        eventTypeRepository.findActiveById(eventTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Event type not found with id: " + eventTypeId));
        return toResponses(eventRepository.findAllActiveByEventTypeIdAndCreatedBy(eventTypeId, createdBy));
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

    // ------------------------------------------------------------------ complexes of the event

    private static Set<UUID> distinctIds(List<UUID> ids) {
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }

    /** A complex the caller cannot see counts as missing: the repository only returns the ones of their organization. */
    private void requireActiveSportComplexes(Set<UUID> sportComplexIds) {
        boolean anyMissing = sportComplexIds.stream().anyMatch(id -> sportComplexRepository.findActiveById(id).isEmpty());
        if (anyMissing) {
            throw new FieldValidationException(SPORT_COMPLEX_IDS_FIELD, EventError.SPORT_COMPLEX_NOT_FOUND.name());
        }
    }

    /** The event ends up in exactly these complexes: the ones it no longer uses are unlinked, the new ones are linked. */
    private void replaceSportComplexes(UUID eventId, Set<UUID> wantedIds) {
        List<EventsSportComplexesEntity> currentLinks = eventsSportComplexesRepository.findAllActiveByEventId(eventId);
        Set<UUID> currentIds = currentLinks.stream().map(EventsSportComplexesEntity::getSportComplexId).collect(Collectors.toSet());

        for (EventsSportComplexesEntity link : currentLinks) {
            if (!wantedIds.contains(link.getSportComplexId())) {
                SoftDeleteHelper.prepareForDeletion(link);
                eventsSportComplexesRepository.delete(link);
            }
        }
        for (UUID wantedId : wantedIds) {
            if (!currentIds.contains(wantedId)) {
                eventsSportComplexesRepository.save(new EventsSportComplexesEntity(eventId, wantedId));
            }
        }
    }

    private EventResponse toResponse(EventsEntity event) {
        return toResponses(List.of(event)).get(0);
    }

    private List<EventResponse> toResponses(List<EventsEntity> events) {
        List<EventResponse> responses = events.stream().map(eventMapper::toResponse).toList();
        attachSportComplexIds(responses);
        return responses;
    }

    /** One query for the whole list, not one per event. */
    private void attachSportComplexIds(List<EventResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }
        Map<UUID, List<UUID>> idsByEvent = eventsSportComplexesRepository
                .findAllActiveByEventIds(responses.stream().map(EventResponse::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(EventsSportComplexesEntity::getEventId,
                        Collectors.mapping(EventsSportComplexesEntity::getSportComplexId, Collectors.toList())));
        responses.forEach(response -> response.setSportComplexIds(idsByEvent.getOrDefault(response.getId(), List.of())));
    }

    private EventFormatsEntity findActiveEventFormat(UUID eventFormatId) {
        return eventFormatRepository.findActiveById(eventFormatId)
                .orElseThrow(() -> new EntityNotFoundException("Event format not found with id: " + eventFormatId));
    }
}
