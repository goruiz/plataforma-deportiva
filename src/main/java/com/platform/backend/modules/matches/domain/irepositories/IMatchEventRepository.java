package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchEventsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchEventRepository {

    MatchEventsEntity save(MatchEventsEntity matchEvent);

    Optional<MatchEventsEntity> findById(UUID id);

    List<MatchEventsEntity> findAllActive();

    Optional<MatchEventsEntity> findActiveById(UUID id);

    void delete(MatchEventsEntity matchEvent);

    void hardDelete(MatchEventsEntity matchEvent);
}
