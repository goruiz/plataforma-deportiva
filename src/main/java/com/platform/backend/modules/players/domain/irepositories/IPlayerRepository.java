package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPlayerRepository {

    PlayersEntity save(PlayersEntity player);

    Optional<PlayersEntity> findActiveById(UUID id);

    Optional<PlayersEntity> findByEmail(String email);

    List<PlayersEntity> findAllActive();

    boolean existsByEmail(String email);

    void hardDelete(PlayersEntity player);

    List<PlayersEntity> findAllActiveByTeamId(UUID teamId);
}
