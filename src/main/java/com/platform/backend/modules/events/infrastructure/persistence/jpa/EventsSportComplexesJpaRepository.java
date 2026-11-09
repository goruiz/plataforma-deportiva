package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventsSportComplexesJpaRepository extends TenantScopedJpaRepository<EventsSportComplexesEntity> {

    List<EventsSportComplexesEntity> findAllByEventIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID eventId);

    List<EventsSportComplexesEntity> findAllByEventIdInAndDeletedAtIsNullOrderByCreatedAtAsc(Collection<UUID> eventIds);
}
