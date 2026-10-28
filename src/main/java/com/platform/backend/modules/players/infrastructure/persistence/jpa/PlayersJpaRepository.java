package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayersJpaRepository extends TenantScopedJpaRepository<PlayersEntity> {

    List<PlayersEntity> findAllByDeletedAtIsNull();

    Optional<PlayersEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<PlayersEntity> findAllByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    Optional<PlayersEntity> findByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    // Native on purpose: a leaderboard or a match timeline names players of other organizations, which the organization
    // filter hides. Only the name is read, never the email or phone, and only of the ids of events the caller can see.
    @Query(value = "SELECT id AS id, first_name AS firstName, last_name AS lastName FROM players "
            + "WHERE id IN (:ids) AND deleted_at IS NULL", nativeQuery = true)
    List<PlayerName> findNamesByIds(@Param("ids") Collection<UUID> ids);

    /** Just enough of a player to name them. */
    interface PlayerName {
        UUID getId();

        String getFirstName();

        String getLastName();
    }

    @Query("SELECT p FROM PlayersEntity p LEFT JOIN FETCH p.team WHERE p.team.id = :teamId AND p.deletedAt IS NULL")
    List<PlayersEntity> findAllByTeamIdWithTeam(@Param("teamId") UUID teamId);
}

