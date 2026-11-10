package com.platform.backend.modules.matches.domain.irepositories;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import com.platform.backend.modules.matches.domain.models.GroupedCount;
import com.platform.backend.modules.matches.domain.models.TeamMatchTally;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The totals of the teams, added up by the database so that a report of every team does not read every match. Every
 * read goes through the organization filter, so a caller only gets what they can see. Matches of an event that was
 * deleted do not count, nor do deleted matches or events of a match.
 */
public interface ITeamStatsRepository {

    /** The matches with a result that each team played at home; a team without any has no line. */
    List<TeamMatchTally> tallyDecidedMatchesAsHome(Collection<UUID> teamIds);

    /** The matches with a result that each team played away; a team without any has no line. */
    List<TeamMatchTally> tallyDecidedMatchesAsAway(Collection<UUID> teamIds);

    /** In how many events each team is enrolled ({@code groupId} is the team). */
    List<GroupedCount> countEnrollmentsByTeam(Collection<UUID> teamIds);

    /** The match events of each team by type ({@code groupId} is the team, {@code label} the event type). */
    List<GroupedCount> countMatchEventsByTeamAndType(Collection<UUID> teamIds);

    /** The match events of the players of one team, by type ({@code groupId} is the player, {@code label} the type). */
    List<GroupedCount> countMatchEventsByPlayerAndType(UUID teamId);

    /** In how many matches each player has an event for the team ({@code groupId} is the player). */
    List<GroupedCount> countMatchesWithEventsByPlayer(UUID teamId);

    /** The matches a team plays, as home or away, the most recent first. */
    PageResult<MatchesEntity> findMatchesOfTeamNewestFirst(UUID teamId, PageQuery pageQuery);
}
