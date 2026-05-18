package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.MatchesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MatchRepository implements IMatchRepository {

    private final MatchesJpaRepository matchesJpaRepository;

    @Override
    public MatchesEntity save(MatchesEntity match) {
        return matchesJpaRepository.save(match);
    }

    @Override
    public Optional<MatchesEntity> findById(UUID id) {
        return matchesJpaRepository.findById(id);
    }

    @Override
    public List<MatchesEntity> findAllActive() {
        return matchesJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<MatchesEntity> findActiveById(UUID id) {
        return matchesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(MatchesEntity match) {
        matchesJpaRepository.save(match);
    }

    @Override
    public void hardDelete(MatchesEntity match) {
        matchesJpaRepository.delete(match);
    }
}
