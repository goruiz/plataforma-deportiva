package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchAttendancesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchAttendancesJpaRepository extends TenantScopedJpaRepository<MatchAttendancesEntity> {

    Optional<MatchAttendancesEntity> findByMatchIdAndPlayerIdAndDeletedAtIsNull(UUID matchId, UUID playerId);

    List<MatchAttendancesEntity> findAllByMatchIdAndDeletedAtIsNull(UUID matchId);
}
