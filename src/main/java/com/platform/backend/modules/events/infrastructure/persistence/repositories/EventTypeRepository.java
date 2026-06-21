package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventTypesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventTypesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventTypeRepository implements IEventTypeRepository {

    private final EventTypesJpaRepository eventTypesJpaRepository;

    @Override
    public EventTypesEntity save(EventTypesEntity eventType) {
        return eventTypesJpaRepository.save(eventType);
    }

    @Override
    public Optional<EventTypesEntity> findById(UUID id) {
        return eventTypesJpaRepository.findById(id);
    }

    @Override
    public List<EventTypesEntity> findAllActive() {
        return eventTypesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<EventTypesEntity> findActiveById(UUID id) {
        return eventTypesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return eventTypesJpaRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void delete(EventTypesEntity eventType) {
        eventTypesJpaRepository.save(eventType);
    }

    @Override
    public void hardDelete(EventTypesEntity eventType) {
        eventTypesJpaRepository.delete(eventType);
    }
}

