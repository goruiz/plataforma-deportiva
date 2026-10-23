package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchEventsJpaRepository extends TenantScopedJpaRepository<MatchEventsEntity> {

    List<MatchEventsEntity> findAllByDeletedAtIsNull();

    Optional<MatchEventsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
