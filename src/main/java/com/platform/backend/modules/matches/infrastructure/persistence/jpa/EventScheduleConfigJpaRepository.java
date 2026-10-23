package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.EventScheduleConfigEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventScheduleConfigJpaRepository extends TenantScopedJpaRepository<EventScheduleConfigEntity> {

    Optional<EventScheduleConfigEntity> findByEventIdAndDeletedAtIsNull(UUID eventId);
}
