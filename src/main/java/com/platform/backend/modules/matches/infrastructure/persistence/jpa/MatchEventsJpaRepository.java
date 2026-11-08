package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchEventsJpaRepository extends TenantScopedJpaRepository<MatchEventsEntity> {

    List<MatchEventsEntity> findAllByDeletedAtIsNull();

    List<MatchEventsEntity> findAllByMatchIdAndDeletedAtIsNull(UUID matchId);

    List<MatchEventsEntity> findAllByMatchIdInAndDeletedAtIsNull(Collection<UUID> matchIds);

    Optional<MatchEventsEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Native on purpose: the organization filter would hide the events a player scored for a team of another
    // organization, and their totals must include them. Only matches that still exist count.
    @Query(value = "SELECT me.* FROM match_events me JOIN matches m ON m.id = me.match_id AND m.deleted_at IS NULL "
            + "WHERE me.player_id = :playerId AND me.deleted_at IS NULL", nativeQuery = true)
    List<MatchEventsEntity> findAllActiveByPlayerId(@Param("playerId") UUID playerId);
}
