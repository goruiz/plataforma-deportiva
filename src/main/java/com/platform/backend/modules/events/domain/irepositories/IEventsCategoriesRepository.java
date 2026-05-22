package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventsCategoriesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventsCategoriesRepository {

    EventsCategoriesEntity save(EventsCategoriesEntity eventsCategories);

    Optional<EventsCategoriesEntity> findById(UUID id);

    List<EventsCategoriesEntity> findAllActive();

    Optional<EventsCategoriesEntity> findActiveById(UUID id);

    void delete(EventsCategoriesEntity eventsCategories);

    void hardDelete(EventsCategoriesEntity eventsCategories);
}
