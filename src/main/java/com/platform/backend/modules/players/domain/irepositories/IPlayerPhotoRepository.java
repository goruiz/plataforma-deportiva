package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.PlayerPhotosEntity;

import java.util.Optional;
import java.util.UUID;

public interface IPlayerPhotoRepository {

    PlayerPhotosEntity save(PlayerPhotosEntity photo);

    Optional<PlayerPhotosEntity> findByPlayerId(UUID playerId);

    /** Erases the photo of the player, bytes included. Does nothing when there is none. */
    void deleteByPlayerId(UUID playerId);
}
