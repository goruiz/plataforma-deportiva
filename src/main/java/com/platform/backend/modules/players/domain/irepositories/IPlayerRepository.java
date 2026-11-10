package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IPlayerRepository {

    PlayersEntity save(PlayersEntity player);

    /** Writes the change right away, for when the order of two updates matters (a unique index). */
    PlayersEntity saveAndFlush(PlayersEntity player);

    Optional<PlayersEntity> findActiveById(UUID id);

    List<PlayersEntity> findAllActiveByIds(Collection<UUID> ids);

    /**
     * The full name of each player, whatever organization the caller sees: for leaderboards and timelines, which name
     * the players of a public event. Players that do not exist are left out.
     */
    Map<UUID, String> findFullNamesByIds(Collection<UUID> ids);

    Optional<PlayersEntity> findByEmail(String email);

    /**
     * The active player with that email whatever organization the caller sees: a player in a team is only visible to the
     * organization of that team, and an administrator of another one still has to be able to tell they are registered.
     * Whoever calls it decides how much of the player to reveal.
     */
    Optional<PlayersEntity> findAnyActiveByEmail(String email);

    List<PlayersEntity> findAllActive();

    boolean existsByEmail(String email);

    void hardDelete(PlayersEntity player);
}
