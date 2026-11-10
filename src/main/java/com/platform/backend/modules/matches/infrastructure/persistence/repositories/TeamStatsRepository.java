package com.platform.backend.modules.matches.infrastructure.persistence.repositories;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.irepositories.ITeamStatsRepository;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.modules.matches.infrastructure.persistence.jpa.TeamStatsJpaRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.pagination.SpringPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class TeamStatsRepository implements ITeamStatsRepository {

    private final TeamStatsJpaRepository teamStatsJpaRepository;

    @Override
    public List<TeamMatchTally> tallyDecidedMatchesAsHome(Collection<UUID> teamIds) {
        return readIfAny(teamIds, teamStatsJpaRepository::tallyDecidedMatchesAsHome, TeamStatsRepository::toTally);
    }

    @Override
    public List<TeamMatchTally> tallyDecidedMatchesAsAway(Collection<UUID> teamIds) {
        return readIfAny(teamIds, teamStatsJpaRepository::tallyDecidedMatchesAsAway, TeamStatsRepository::toTally);
    }

    @Override
    public List<GroupedCount> countEnrollmentsByTeam(Collection<UUID> teamIds) {
        return readIfAny(teamIds, teamStatsJpaRepository::countEnrollmentsByTeam,
                row -> new GroupedCount((UUID) row[0], null, number(row[1])));
    }

    @Override
    public List<GroupedCount> countMatchEventsByTeamAndType(Collection<UUID> teamIds) {
        return readIfAny(teamIds, teamStatsJpaRepository::countMatchEventsByTeamAndType, TeamStatsRepository::toCountByType);
    }

    @Override
    public List<GroupedCount> countMatchEventsByPlayerAndType(UUID teamId) {
        return teamStatsJpaRepository.countMatchEventsByPlayerAndType(teamId).stream()
                .map(TeamStatsRepository::toCountByType)
                .toList();
    }

    @Override
    public List<GroupedCount> countMatchesWithEventsByPlayer(UUID teamId) {
        return teamStatsJpaRepository.countMatchesWithEventsByPlayer(teamId).stream()
                .map(row -> new GroupedCount((UUID) row[0], null, number(row[1])))
                .toList();
    }

    @Override
    public PageResult<MatchesEntity> findMatchesOfTeamNewestFirst(UUID teamId, PageQuery pageQuery) {
        // Unsorted on purpose: the query orders by date itself.
        return SpringPagination.toResult(
                teamStatsJpaRepository.findMatchesOfTeamNewestFirst(teamId, SpringPagination.toPageable(pageQuery, Sort.unsorted())),
                pageQuery);
    }

    /** An empty list of ids would be an invalid {@code IN ()}, and has nothing to read anyway. */
    private static <T> List<T> readIfAny(Collection<UUID> teamIds,
                                         Function<Collection<UUID>, List<Object[]>> query,
                                         Function<Object[], T> toRecord) {
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return query.apply(teamIds).stream().map(toRecord).toList();
    }

    private static TeamMatchTally toTally(Object[] row) {
        return new TeamMatchTally((UUID) row[0], number(row[1]), number(row[2]), number(row[3]), number(row[4]),
                number(row[5]), number(row[6]));
    }

    private static GroupedCount toCountByType(Object[] row) {
        return new GroupedCount((UUID) row[0], (String) row[1], number(row[2]));
    }

    /** SUM and COUNT come back as Long or Integer depending on the column, and SUM of nothing is null. */
    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
