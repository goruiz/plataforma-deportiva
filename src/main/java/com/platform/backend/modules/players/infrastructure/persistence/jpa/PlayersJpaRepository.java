package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayersJpaRepository extends JpaRepository<PlayersEntity, UUID> {

    List<PlayersEntity> findAllByDeletedAtIsNull();

    Optional<PlayersEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PlayersEntity> findByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT p FROM PlayersEntity p LEFT JOIN FETCH p.team WHERE p.team.id = :teamId AND p.deletedAt IS NULL")
    List<PlayersEntity> findAllByTeamIdWithTeam(@Param("teamId") UUID teamId);
}

