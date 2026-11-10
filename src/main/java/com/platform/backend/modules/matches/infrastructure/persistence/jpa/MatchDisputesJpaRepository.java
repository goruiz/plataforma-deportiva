package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchDisputesJpaRepository
        extends TenantScopedJpaRepository<MatchDisputesEntity>, JpaSpecificationExecutor<MatchDisputesEntity> {

    Optional<MatchDisputesEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<MatchDisputesEntity> findAllByMatchIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID matchId);

    List<MatchDisputesEntity> findAllByTeamIdInAndDeletedAtIsNullOrderByCreatedAtDesc(Collection<UUID> teamIds);

    boolean existsByMatchIdAndTeamIdAndStatusAndDeletedAtIsNull(UUID matchId, UUID teamId, String status);

    boolean existsByMatchIdAndStatusAndDeletedAtIsNull(UUID matchId, String status);

    /** Rows of {@code [matchId, count]}: only the matches that have an open dispute. */
    @Query("SELECT d.matchId, COUNT(d) FROM MatchDisputesEntity d "
            + "WHERE d.status = :status AND d.deletedAt IS NULL GROUP BY d.matchId")
    List<Object[]> countByMatchWithStatus(@Param("status") String status);
}
