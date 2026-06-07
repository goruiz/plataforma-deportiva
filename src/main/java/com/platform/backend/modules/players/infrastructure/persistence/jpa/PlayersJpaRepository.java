package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayersJpaRepository extends JpaRepository<PlayersEntity, UUID> {

    List<PlayersEntity> findAllByDeletedAtIsNull();

    Optional<PlayersEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PlayersEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<PlayersEntity> findAllByTeam_IdAndDeletedAtIsNull(UUID teamId);
}
