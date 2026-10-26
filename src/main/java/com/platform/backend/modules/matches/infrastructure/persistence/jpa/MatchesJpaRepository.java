package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchesJpaRepository extends TenantScopedJpaRepository<MatchesEntity> {

    List<MatchesEntity> findAllByDeletedAtIsNull();

    Optional<MatchesEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<MatchesEntity> findAllByEventIdAndDeletedAtIsNull(UUID eventId);

    @Query("SELECT m FROM MatchesEntity m WHERE (m.homeTeamId = :teamId OR m.awayTeamId = :teamId) "
            + "AND m.deletedAt IS NULL ORDER BY m.matchDate")
    List<MatchesEntity> findAllActiveByTeamId(@Param("teamId") UUID teamId);

    List<MatchesEntity> findAllByStatusInAndMatchDateLessThanEqualAndDeletedAtIsNull(
            Collection<String> statuses, LocalDateTime matchDate);

    List<MatchesEntity> findAllByEventIdAndMatchDateBetweenAndDeletedAtIsNull(
            UUID eventId, LocalDateTime start, LocalDateTime end);
}
