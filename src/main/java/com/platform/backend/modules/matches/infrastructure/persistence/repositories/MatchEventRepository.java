package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;
import com.platform.backend.modules.matches.domain.irepositories.IMatchEventRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.MatchEventsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MatchEventRepository implements IMatchEventRepository {

    private final MatchEventsJpaRepository matchEventsJpaRepository;

    @Override
    public MatchEventsEntity save(MatchEventsEntity matchEvent) {
        return matchEventsJpaRepository.save(matchEvent);
    }

    @Override
    public Optional<MatchEventsEntity> findById(UUID id) {
        return matchEventsJpaRepository.findById(id);
    }

    @Override
    public List<MatchEventsEntity> findAllActive() {
        return matchEventsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<MatchEventsEntity> findActiveById(UUID id) {
        return matchEventsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(MatchEventsEntity matchEvent) {
        matchEventsJpaRepository.save(matchEvent);
    }

    @Override
    public void hardDelete(MatchEventsEntity matchEvent) {
        matchEventsJpaRepository.delete(matchEvent);
    }
}
