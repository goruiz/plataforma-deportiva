package com.platform.backend.modules.teams.domain.irepositories;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamRepository {

    TeamsEntity save(TeamsEntity team);

    Optional<TeamsEntity> findById(UUID id);

    List<TeamsEntity> findAllActive();
    
    List<TeamsEntity> findAllActiveByIds(Collection<UUID> ids);


    Optional<TeamsEntity> findActiveById(UUID id);

    boolean existsByName(String name);

    void delete(TeamsEntity team);

    void hardDelete(TeamsEntity team);

}
