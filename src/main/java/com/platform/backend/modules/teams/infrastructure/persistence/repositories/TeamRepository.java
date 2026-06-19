package com.platform.backend.modules.teams.infrastructure.persistence.repositories;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.infrastructure.persistence.jpa.TeamsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TeamRepository implements ITeamRepository {

    private final TeamsJpaRepository teamsJpaRepository;

    @Override
    public TeamsEntity save(TeamsEntity team) {
        return teamsJpaRepository.save(team);
    }

    @Override
    public Optional<TeamsEntity> findById(UUID id) {
        return teamsJpaRepository.findById(id);
    }

    @Override
    public List<TeamsEntity> findAllActive() {
        return teamsJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public Optional<TeamsEntity> findActiveById(UUID id) {
        return teamsJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByName(String name) {
        return teamsJpaRepository.existsByName(name);
    }

    @Override
    public void delete(TeamsEntity team) {
        teamsJpaRepository.save(team);
    }

    @Override
    public void hardDelete(TeamsEntity team) {
        teamsJpaRepository.delete(team);
    }

    @Override
    public List<TeamsEntity> findAllActiveByIds(Collection<UUID> ids) {
        return teamsJpaRepository.findAllActiveByIds(ids);
    }
    
}
