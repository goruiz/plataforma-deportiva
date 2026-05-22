package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventRepository {

    EventsEntity save(EventsEntity event);

    Optional<EventsEntity> findById(UUID id);

    List<EventsEntity> findAllActive();

    Optional<EventsEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(EventsEntity event);

    void hardDelete(EventsEntity event);
}
