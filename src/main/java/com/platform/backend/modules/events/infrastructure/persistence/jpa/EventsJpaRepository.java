package com.platform.backend.modules.events.infrastructure.persistence.jpa;

import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsJpaRepository extends TenantScopedJpaRepository<EventsEntity> {

    List<EventsEntity> findAllByDeletedAtIsNull();

    Optional<EventsEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    List<EventsEntity> findAllByEventTypeIdAndDeletedAtIsNull(UUID eventTypeId);

    List<EventsEntity> findAllByEventTypeIdAndCreatedByAndDeletedAtIsNull(UUID eventTypeId, UUID createdBy);

    boolean existsByEventFormatIdAndDeletedAtIsNull(UUID eventFormatId);

    boolean existsByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
}
