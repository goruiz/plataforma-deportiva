package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsJpaRepository extends JpaRepository<EventsEntity, UUID> {

    List<EventsEntity> findAllByDeletedAtIsNull();

    Optional<EventsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByName(String name);

    List<EventsEntity> findAllByEventTypeIdAndDeletedAtIsNull(UUID eventTypeId);

    List<EventsEntity> findAllByEventTypeIdAndCreatedByAndDeletedAtIsNull(UUID eventTypeId, UUID createdBy);
}
