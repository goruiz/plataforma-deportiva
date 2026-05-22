package com.platform.backend.modules.teams.domain.irepositories;

import com.platform.backend.modules.teams.domain.entities.TeamsEventsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamsEventsRepository {

    TeamsEventsEntity save(TeamsEventsEntity teamsEvents);

    Optional<TeamsEventsEntity> findById(UUID id);

    List<TeamsEventsEntity> findAllActive();

    Optional<TeamsEventsEntity> findActiveById(UUID id);

    void delete(TeamsEventsEntity teamsEvents);

    void hardDelete(TeamsEventsEntity teamsEvents);
}
