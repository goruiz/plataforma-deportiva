package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchDetailsJpaRepository extends TenantScopedJpaRepository<MatchDetailsEntity> {

    List<MatchDetailsEntity> findAllByDeletedAtIsNull();

    Optional<MatchDetailsEntity> findByIdAndDeletedAtIsNull(UUID id);
}
