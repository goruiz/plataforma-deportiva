package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventTypesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventTypeRepository {

    EventTypesEntity save(EventTypesEntity eventType);

    Optional<EventTypesEntity> findById(UUID id);

    List<EventTypesEntity> findAllActive();

    Optional<EventTypesEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(EventTypesEntity eventType);

    void hardDelete(EventTypesEntity eventType);
}
