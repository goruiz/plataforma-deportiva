package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventRepository implements IEventRepository {

    private final EventsJpaRepository eventsJpaRepository;

    @Override
    public EventsEntity save(EventsEntity event) {
        return eventsJpaRepository.save(event);
    }

    @Override
    public Optional<EventsEntity> findById(UUID id) {
        return eventsJpaRepository.findById(id);
    }

    @Override
    public List<EventsEntity> findAllActive() {
        return eventsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<EventsEntity> findActiveById(UUID id) {
        return eventsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return eventsJpaRepository.existsByName(name);
    }

    @Override
    public List<EventsEntity> findAllActiveByEventTypeId(UUID eventTypeId) {
        return eventsJpaRepository.findAllByEventTypeIdAndDeletedAtIsNull(eventTypeId);
    }

    @Override
    public List<EventsEntity> findAllActiveByEventTypeIdAndCreatedBy(UUID eventTypeId, UUID createdBy) {
        return eventsJpaRepository.findAllByEventTypeIdAndCreatedByAndDeletedAtIsNull(eventTypeId, createdBy);
    }

    @Override
    public void delete(EventsEntity event) {
        eventsJpaRepository.save(event);
    }

    @Override
    public void hardDelete(EventsEntity event) {
        eventsJpaRepository.delete(event);
    }
}
