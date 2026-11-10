package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchDisputesEntity;
import com.platform.backend.modules.matches.domain.enums.DisputeStatus;
import com.platform.backend.modules.matches.domain.filters.DisputeFilter;
import com.platform.backend.modules.matches.domain.irepositories.IMatchDisputeRepository;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.DisputeSpecifications;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.MatchDisputesJpaRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.pagination.SpringPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MatchDisputeRepository implements IMatchDisputeRepository {

    private final MatchDisputesJpaRepository matchDisputesJpaRepository;

    @Override
    public MatchDisputesEntity save(MatchDisputesEntity dispute) {
        return matchDisputesJpaRepository.save(dispute);
    }

    @Override
    public Optional<MatchDisputesEntity> findActiveById(UUID id) {
        return matchDisputesJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public PageResult<MatchDisputesEntity> findPage(DisputeFilter filter, PageQuery pageQuery) {
        // The id breaks ties so a page never repeats or skips a row that shares its creation second.
        Pageable pageable = SpringPagination.toPageable(pageQuery,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        return SpringPagination.toResult(
                matchDisputesJpaRepository.findAll(DisputeSpecifications.matching(filter), pageable), pageQuery);
    }

    @Override
    public long count(DisputeFilter filter) {
        return matchDisputesJpaRepository.count(DisputeSpecifications.matching(filter));
    }

    @Override
    public List<MatchDisputesEntity> findAllActiveByMatchId(UUID matchId) {
        return matchDisputesJpaRepository.findAllByMatchIdAndDeletedAtIsNullOrderByCreatedAtDesc(matchId);
    }

    @Override
    public List<MatchDisputesEntity> findAllActiveByTeamIds(Collection<UUID> teamIds) {
        return teamIds.isEmpty()
                ? List.of()
                : matchDisputesJpaRepository.findAllByTeamIdInAndDeletedAtIsNullOrderByCreatedAtDesc(teamIds);
    }

    @Override
    public boolean existsOpenByMatchIdAndTeamId(UUID matchId, UUID teamId) {
        return matchDisputesJpaRepository.existsByMatchIdAndTeamIdAndStatusAndDeletedAtIsNull(
                matchId, teamId, DisputeStatus.OPEN.name());
    }

    @Override
    public boolean existsOpenByMatchId(UUID matchId) {
        return matchDisputesJpaRepository.existsByMatchIdAndStatusAndDeletedAtIsNull(matchId, DisputeStatus.OPEN.name());
    }

    @Override
    public Map<UUID, Integer> countOpenByMatch() {
        Map<UUID, Integer> counts = new HashMap<>();
        matchDisputesJpaRepository.countByMatchWithStatus(DisputeStatus.OPEN.name())
                .forEach(row -> counts.put((UUID) row[0], ((Number) row[1]).intValue()));
        return counts;
    }
}
