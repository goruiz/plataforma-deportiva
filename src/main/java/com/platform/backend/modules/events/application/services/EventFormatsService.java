package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventFormatRulesService;
import com.platform.backend.modules.events.application.iservices.IEventFormatsService;
import com.platform.backend.modules.events.application.mappers.EventFormatMapper;
import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;
import com.platform.backend.modules.events.domain.exception.EventFormatError;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventFormatRequest.CreateEventFormatRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest.UpdateEventFormatRequest;
import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventFormatsService implements IEventFormatsService {

    private static final String NAME_FIELD = "name";

    private final IEventFormatRepository eventFormatRepository;
    private final IEventRepository eventRepository;
    private final EventFormatMapper eventFormatMapper;
    private final IEventFormatRulesService eventFormatRulesService;

    @Transactional(readOnly = true)
    @Override
    public List<EventFormatResponse> getAll() {
        return eventFormatRepository.findAllActive()
                .stream()
                .map(eventFormatMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public EventFormatResponse getById(UUID id) {
        return eventFormatMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public EventFormatResponse create(CreateEventFormatRequest request) {
        String name = request.getName().trim();
        if (eventFormatRepository.existsByName(name)) {
            throw nameAlreadyExists();
        }
        EventFormatsEntity eventFormat = eventFormatMapper.toEntity(request);
        eventFormat.setName(name);
        eventFormatRulesService.validateAndNormalize(eventFormat);
        return eventFormatMapper.toResponse(eventFormatRepository.save(eventFormat));
    }

    @Transactional
    @Override
    public EventFormatResponse update(UUID id, UpdateEventFormatRequest request) {
        EventFormatsEntity eventFormat = findActiveOrThrow(id);
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (eventFormatRepository.existsByNameExcludingId(name, id)) {
                throw nameAlreadyExists();
            }
            request.setName(name);
        }
        eventFormatMapper.updateEntity(eventFormat, request);
        // The update only changes what was sent, so the rules are judged as the whole set they now form.
        eventFormatRulesService.validateAndNormalize(eventFormat);
        return eventFormatMapper.toResponse(eventFormatRepository.save(eventFormat));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        EventFormatsEntity eventFormat = findActiveOrThrow(id);
        if (eventRepository.existsActiveByEventFormatId(id)) {
            throw new IllegalStateException(EventFormatError.IN_USE.name());
        }
        SoftDeleteHelper.prepareForDeletion(eventFormat);
        eventFormatRepository.delete(eventFormat);
    }

    private EventFormatsEntity findActiveOrThrow(UUID id) {
        return eventFormatRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event format not found with id: " + id));
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, EventFormatError.NAME_ALREADY_EXISTS.name());
    }
}
