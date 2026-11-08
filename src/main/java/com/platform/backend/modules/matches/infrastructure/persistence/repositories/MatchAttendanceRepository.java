package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchAttendancesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchAttendanceRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.MatchAttendancesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MatchAttendanceRepository implements IMatchAttendanceRepository {

    private final MatchAttendancesJpaRepository matchAttendancesJpaRepository;

    @Override
    public MatchAttendancesEntity save(MatchAttendancesEntity attendance) {
        return matchAttendancesJpaRepository.save(attendance);
    }

    @Override
    public Optional<MatchAttendancesEntity> findActiveByMatchIdAndPlayerId(UUID matchId, UUID playerId) {
        return matchAttendancesJpaRepository.findByMatchIdAndPlayerIdAndDeletedAtIsNull(matchId, playerId);
    }

    @Override
    public List<MatchAttendancesEntity> findAllActiveByMatchId(UUID matchId) {
        return matchAttendancesJpaRepository.findAllByMatchIdAndDeletedAtIsNull(matchId);
    }

    @Override
    public void delete(MatchAttendancesEntity attendance) {
        matchAttendancesJpaRepository.save(attendance);
    }
}
