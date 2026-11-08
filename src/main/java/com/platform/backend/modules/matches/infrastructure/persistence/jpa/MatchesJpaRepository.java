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

    @Query("SELECT m FROM MatchesEntity m WHERE m.courtId = :courtId AND m.deletedAt IS NULL ORDER BY m.matchDate")
    List<MatchesEntity> findAllActiveByCourtId(@Param("courtId") UUID courtId);

    @Query("SELECT m FROM MatchesEntity m WHERE m.courtId = :courtId AND m.matchDate >= :from "
            + "AND m.deletedAt IS NULL ORDER BY m.matchDate")
    List<MatchesEntity> findAllActiveByCourtIdFrom(@Param("courtId") UUID courtId, @Param("from") LocalDateTime from);

    // Half open: a match at midnight sharp belongs to the day that starts then, not to the one that ends.
    @Query("SELECT m FROM MatchesEntity m WHERE m.courtId = :courtId AND m.matchDate >= :start AND m.matchDate < :end "
            + "AND m.deletedAt IS NULL ORDER BY m.matchDate")
    List<MatchesEntity> findAllActiveByCourtIdBetween(@Param("courtId") UUID courtId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    List<MatchesEntity> findAllByStatusInAndMatchDateLessThanEqualAndDeletedAtIsNull(
            Collection<String> statuses, LocalDateTime matchDate);

    List<MatchesEntity> findAllByEventIdAndMatchDateBetweenAndDeletedAtIsNull(
            UUID eventId, LocalDateTime start, LocalDateTime end);
}
