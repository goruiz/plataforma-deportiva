package com.platform.backend.modules.teams.infrastructure.persistence.jpa;

import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamsEventsJpaRepository extends TenantScopedJpaRepository<TeamsEventsEntity> {

    List<TeamsEventsEntity> findAllByDeletedAtIsNull();

    Optional<TeamsEventsEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<TeamsEventsEntity> findAllByEvent_IdAndDeletedAtIsNull(UUID eventId);
}
