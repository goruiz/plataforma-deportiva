package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchAttendancesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchAttendanceRepository {

    MatchAttendancesEntity save(MatchAttendancesEntity attendance);

    /** The active answer of a player to a match, if they gave one. */
    Optional<MatchAttendancesEntity> findActiveByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    List<MatchAttendancesEntity> findAllActiveByMatchId(UUID matchId);

    /** Soft delete: the caller has already stamped {@code deletedAt}. */
    void delete(MatchAttendancesEntity attendance);
}
