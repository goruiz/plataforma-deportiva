package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Every read goes through the organization filter, so an administrator only sees the disputes of their own matches. */
public interface IMatchDisputeRepository {

    MatchDisputesEntity save(MatchDisputesEntity dispute);

    Optional<MatchDisputesEntity> findActiveById(UUID id);

    /** One page of the active disputes that match the filter, the most recent first. */
    PageResult<MatchDisputesEntity> findPage(DisputeFilter filter, PageQuery pageQuery);

    /** How many active disputes match the filter (its status included). */
    long count(DisputeFilter filter);

    List<MatchDisputesEntity> findAllActiveByMatchId(UUID matchId);

    List<MatchDisputesEntity> findAllActiveByTeamIds(Collection<UUID> teamIds);

    boolean existsOpenByMatchIdAndTeamId(UUID matchId, UUID teamId);

    boolean existsOpenByMatchId(UUID matchId);

    /** How many disputes are open for each match that has any (a match without one is not in the map). */
    Map<UUID, Integer> countOpenByMatch();
}
