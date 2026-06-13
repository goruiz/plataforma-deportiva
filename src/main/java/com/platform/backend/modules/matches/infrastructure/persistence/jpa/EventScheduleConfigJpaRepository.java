package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventScheduleConfigJpaRepository extends JpaRepository<EventScheduleConfigEntity, UUID> {

    Optional<EventScheduleConfigEntity> findByEventIdAndDeletedAtIsNull(UUID eventId);
}
