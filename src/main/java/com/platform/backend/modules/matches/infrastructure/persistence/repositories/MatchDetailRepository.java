package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchDetailRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.MatchDetailsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MatchDetailRepository implements IMatchDetailRepository {

    private final MatchDetailsJpaRepository matchDetailsJpaRepository;

    @Override
    public MatchDetailsEntity save(MatchDetailsEntity matchDetail) {
        return matchDetailsJpaRepository.save(matchDetail);
    }

    @Override
    public Optional<MatchDetailsEntity> findById(UUID id) {
        return matchDetailsJpaRepository.findById(id);
    }

    @Override
    public List<MatchDetailsEntity> findAllActive() {
        return matchDetailsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<MatchDetailsEntity> findActiveById(UUID id) {
        return matchDetailsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(MatchDetailsEntity matchDetail) {
        matchDetailsJpaRepository.save(matchDetail);
    }

    @Override
    public void hardDelete(MatchDetailsEntity matchDetail) {
        matchDetailsJpaRepository.delete(matchDetail);
    }
}
