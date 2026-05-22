package com.platform.backend.modules.events.infrastructure.persistence.repositories;

import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventsCategoriesRepository;
import com.platform.backend.modules.events.infrastructure.persistence.jpa.EventsCategoriesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventsCategoriesRepository implements IEventsCategoriesRepository {

    private final EventsCategoriesJpaRepository eventsCategoriesJpaRepository;

    @Override
    public EventsCategoriesEntity save(EventsCategoriesEntity eventsCategories) {
        return eventsCategoriesJpaRepository.save(eventsCategories);
    }

    @Override
    public Optional<EventsCategoriesEntity> findById(UUID id) {
        return eventsCategoriesJpaRepository.findById(id);
    }

    @Override
    public List<EventsCategoriesEntity> findAllActive() {
        return eventsCategoriesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<EventsCategoriesEntity> findActiveById(UUID id) {
        return eventsCategoriesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(EventsCategoriesEntity eventsCategories) {
        eventsCategoriesJpaRepository.save(eventsCategories);
    }

    @Override
    public void hardDelete(EventsCategoriesEntity eventsCategories) {
        eventsCategoriesJpaRepository.delete(eventsCategories);
    }
}
