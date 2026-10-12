package com.platform.backend.modules.events.domain.irepositories;

import com.platform.backend.modules.events.domain.entities.EventFormatsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventFormatRepository {

    EventFormatsEntity save(EventFormatsEntity eventFormat);

    List<EventFormatsEntity> findAllActive();

    Optional<EventFormatsEntity> findActiveById(UUID id);

    /** Names are compared ignoring case. */
    boolean existsByName(String name);

    /** True when an active format other than {@code id} already uses {@code name} (ignoring case). */
    boolean existsByNameExcludingId(String name, UUID id);

    void delete(EventFormatsEntity eventFormat);
}
