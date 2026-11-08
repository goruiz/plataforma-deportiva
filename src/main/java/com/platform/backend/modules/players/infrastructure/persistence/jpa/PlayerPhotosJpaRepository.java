package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.PlayerPhotosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// No organization filter on purpose: who may read a photo is decided by looking the player up first (that lookup is
// filtered), and a photo is never reached by its own id.
public interface PlayerPhotosJpaRepository extends JpaRepository<PlayerPhotosEntity, UUID> {

    Optional<PlayerPhotosEntity> findByPlayerId(UUID playerId);

    // A bulk delete: the bytes go at once, without loading them first. Nothing keeps a photo loaded while it runs, so the
    // persistence context is left alone (clearing it would detach the player and account being deleted alongside).
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PlayerPhotosEntity photo WHERE photo.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") UUID playerId);
}
