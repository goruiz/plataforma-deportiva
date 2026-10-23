package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchesJpaRepository extends TenantScopedJpaRepository<MatchesEntity> {

    List<MatchesEntity> findAllByDeletedAtIsNull();

    Optional<MatchesEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<MatchesEntity> findAllByEventIdAndDeletedAtIsNull(UUID eventId);

    List<MatchesEntity> findAllByStatusInAndMatchDateLessThanEqualAndDeletedAtIsNull(
            Collection<String> statuses, LocalDateTime matchDate);

    List<MatchesEntity> findAllByEventIdAndMatchDateBetweenAndDeletedAtIsNull(
            UUID eventId, LocalDateTime start, LocalDateTime end);
}
