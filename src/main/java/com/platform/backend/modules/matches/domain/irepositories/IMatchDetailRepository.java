package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchDetailsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchDetailRepository {

    MatchDetailsEntity save(MatchDetailsEntity matchDetail);

    Optional<MatchDetailsEntity> findById(UUID id);

    List<MatchDetailsEntity> findAllActive();

    Optional<MatchDetailsEntity> findActiveById(UUID id);

    void delete(MatchDetailsEntity matchDetail);

    void hardDelete(MatchDetailsEntity matchDetail);
}
