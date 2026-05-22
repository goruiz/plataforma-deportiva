package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.iservices.IEventsCategoriesService;
import com.platform.backend.modules.events.application.mappers.EventsCategoriesMapper;
import com.platform.backend.modules.events.domain.entities.CategoriesEntity;
import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsCategoriesRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventsCategoriesRequest.CreateEventsCategoriesRequest;
import com.platform.backend.modules.events.presentation.responses.EventsCategoriesResponse.EventsCategoriesResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventsCategoriesService implements IEventsCategoriesService {

    private final IEventsCategoriesRepository eventsCategoriesRepository;
    private final IEventRepository eventRepository;
    private final ICategoryRepository categoryRepository;
    private final EventsCategoriesMapper eventsCategoriesMapper;

    @Override
    public List<EventsCategoriesResponse> getAll() {
        return eventsCategoriesRepository.findAllActive()
                .stream()
                .map(eventsCategoriesMapper::toResponse)
                .toList();
    }

    @Override
    public EventsCategoriesResponse getById(UUID id) {
        EventsCategoriesEntity eventsCategories = eventsCategoriesRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event-category assignment not found with id: " + id));
        return eventsCategoriesMapper.toResponse(eventsCategories);
    }

    @Override
    public EventsCategoriesResponse create(CreateEventsCategoriesRequest request) {
        EventsEntity event = eventRepository.findActiveById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + request.getEventId()));
        CategoriesEntity category = categoryRepository.findActiveById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));

        EventsCategoriesEntity eventsCategories = new EventsCategoriesEntity();
        eventsCategories.setEvent(event);
        eventsCategories.setCategory(category);

        return eventsCategoriesMapper.toResponse(eventsCategoriesRepository.save(eventsCategories));
    }

    @Override
    public void delete(UUID id) {
        EventsCategoriesEntity eventsCategories = eventsCategoriesRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event-category assignment not found with id: " + id));
        eventsCategories.setDeletedAt(LocalDateTime.now());
        eventsCategoriesRepository.delete(eventsCategories);
    }

    @Override
    public void hardDelete(UUID id) {
        EventsCategoriesEntity eventsCategories = eventsCategoriesRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event-category assignment not found with id: " + id));
        eventsCategoriesRepository.hardDelete(eventsCategories);
    }
}
