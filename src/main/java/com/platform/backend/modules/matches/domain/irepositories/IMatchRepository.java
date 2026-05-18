package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchRepository {

    MatchesEntity save(MatchesEntity match);

    Optional<MatchesEntity> findById(UUID id);

    List<MatchesEntity> findAllActive();

    Optional<MatchesEntity> findActiveById(UUID id);

    void delete(MatchesEntity match);

    void hardDelete(MatchesEntity match);
}
