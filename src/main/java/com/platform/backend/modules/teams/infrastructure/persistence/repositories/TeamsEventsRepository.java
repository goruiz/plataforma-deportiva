package com.platform.backend.modules.teams.infrastructure.persistence.repositories;

import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamsEventsRepository;
import com.platform.backend.modules.teams.infrastructure.persistence.jpa.TeamsEventsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TeamsEventsRepository implements ITeamsEventsRepository {

    private final TeamsEventsJpaRepository teamsEventsJpaRepository;

    @Override
    public TeamsEventsEntity save(TeamsEventsEntity teamsEvents) {
        return teamsEventsJpaRepository.save(teamsEvents);
    }

    @Override
    public Optional<TeamsEventsEntity> findById(UUID id) {
        return teamsEventsJpaRepository.findById(id);
    }

    @Override
    public List<TeamsEventsEntity> findAllActive() {
        return teamsEventsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<TeamsEventsEntity> findActiveById(UUID id) {
        return teamsEventsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(TeamsEventsEntity teamsEvents) {
        teamsEventsJpaRepository.save(teamsEvents);
    }

    @Override
    public void hardDelete(TeamsEventsEntity teamsEvents) {
        teamsEventsJpaRepository.delete(teamsEvents);
    }
}
