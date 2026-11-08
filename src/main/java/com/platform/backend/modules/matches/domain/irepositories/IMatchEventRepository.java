package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchEventRepository {

    MatchEventsEntity save(MatchEventsEntity matchEvent);

    Optional<MatchEventsEntity> findById(UUID id);

    List<MatchEventsEntity> findAllActive();

    List<MatchEventsEntity> findAllActiveByMatchId(UUID matchId);

    List<MatchEventsEntity> findAllActiveByMatchIds(Collection<UUID> matchIds);

    /**
     * Every active event of a player in every match of every event, whichever organization those events belong to: a
     * player's totals include the events of the teams they left. The organization filter does not apply, so only call
     * it for a player the caller is already allowed to see.
     */
    List<MatchEventsEntity> findAllActiveByPlayerId(UUID playerId);

    Optional<MatchEventsEntity> findActiveById(UUID id);

    void delete(MatchEventsEntity matchEvent);

    void hardDelete(MatchEventsEntity matchEvent);
}
