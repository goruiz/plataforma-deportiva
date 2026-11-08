package com.platform.backend.modules.players.infrastructure.persistence.repositories;

import com.platform.backend.modules.players.domain.entities.PlayerPhotosEntity;
import com.platform.backend.modules.players.domain.irepositories.IPlayerPhotoRepository;
import com.platform.backend.modules.players.infrastructure.persistence.jpa.PlayerPhotosJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlayerPhotoRepository implements IPlayerPhotoRepository {

    private final PlayerPhotosJpaRepository playerPhotosJpaRepository;

    @Override
    public PlayerPhotosEntity save(PlayerPhotosEntity photo) {
        return playerPhotosJpaRepository.save(photo);
    }

    @Override
    public Optional<PlayerPhotosEntity> findByPlayerId(UUID playerId) {
        return playerPhotosJpaRepository.findByPlayerId(playerId);
    }

    @Override
    public void deleteByPlayerId(UUID playerId) {
        playerPhotosJpaRepository.deleteByPlayerId(playerId);
    }
}
