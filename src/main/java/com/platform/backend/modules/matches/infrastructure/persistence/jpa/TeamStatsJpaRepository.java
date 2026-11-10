package com.platform.backend.modules.matches.infrastructure.persistence.jpa;

import com.platform.backend.modules.matches.domain.entities.MatchesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Grouped counts over matches, match events and enrollments. It is a plain {@link Repository} (no {@code findById}) and
 * only has JPQL, so the organization filter of each entity applies to every line it reads.
 */
public interface TeamStatsJpaRepository extends Repository<MatchesEntity, UUID> {

    // A match that was deleted does not count, and neither does one of an event that was deleted (the event is only
    // marked as deleted: its matches stay). A match without an event (the oldest ones) always counts.
    String ACTIVE_MATCH = "m.deletedAt IS NULL AND (m.eventId IS NULL OR EXISTS "
            + "(SELECT 1 FROM EventsEntity ev WHERE ev.id = m.eventId AND ev.deletedAt IS NULL))";

    String HAS_RESULT = "m.homeScore IS NOT NULL AND m.awayScore IS NOT NULL";

    // Columns: team, played, wins, draws, losses, goals for, goals against.
    @Query("SELECT m.homeTeamId, COUNT(m), "
            + "SUM(CASE WHEN m.homeScore > m.awayScore THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN m.homeScore = m.awayScore THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN m.homeScore < m.awayScore THEN 1 ELSE 0 END), "
            + "SUM(m.homeScore), SUM(m.awayScore) "
            + "FROM MatchesEntity m WHERE m.homeTeamId IN :teamIds AND " + HAS_RESULT + " AND " + ACTIVE_MATCH
            + " GROUP BY m.homeTeamId")
    List<Object[]> tallyDecidedMatchesAsHome(@Param("teamIds") Collection<UUID> teamIds);

    @Query("SELECT m.awayTeamId, COUNT(m), "
            + "SUM(CASE WHEN m.awayScore > m.homeScore THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN m.awayScore = m.homeScore THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN m.awayScore < m.homeScore THEN 1 ELSE 0 END), "
            + "SUM(m.awayScore), SUM(m.homeScore) "
            + "FROM MatchesEntity m WHERE m.awayTeamId IN :teamIds AND " + HAS_RESULT + " AND " + ACTIVE_MATCH
            + " GROUP BY m.awayTeamId")
    List<Object[]> tallyDecidedMatchesAsAway(@Param("teamIds") Collection<UUID> teamIds);

    @Query("SELECT te.team.id, COUNT(te) FROM TeamsEventsEntity te "
            + "WHERE te.team.id IN :teamIds AND te.deletedAt IS NULL AND te.event.deletedAt IS NULL "
            + "GROUP BY te.team.id")
    List<Object[]> countEnrollmentsByTeam(@Param("teamIds") Collection<UUID> teamIds);

    @Query("SELECT e.teamId, e.eventType, COUNT(e) FROM MatchEventsEntity e "
            + "WHERE e.teamId IN :teamIds AND e.deletedAt IS NULL "
            + "AND e.matchId IN (SELECT m.id FROM MatchesEntity m WHERE " + ACTIVE_MATCH + ") "
            + "GROUP BY e.teamId, e.eventType")
    List<Object[]> countMatchEventsByTeamAndType(@Param("teamIds") Collection<UUID> teamIds);

    @Query("SELECT e.playerId, e.eventType, COUNT(e) FROM MatchEventsEntity e "
            + "WHERE e.teamId = :teamId AND e.deletedAt IS NULL "
            + "AND e.matchId IN (SELECT m.id FROM MatchesEntity m WHERE " + ACTIVE_MATCH + ") "
            + "GROUP BY e.playerId, e.eventType")
    List<Object[]> countMatchEventsByPlayerAndType(@Param("teamId") UUID teamId);

    @Query("SELECT e.playerId, COUNT(DISTINCT e.matchId) FROM MatchEventsEntity e "
            + "WHERE e.teamId = :teamId AND e.deletedAt IS NULL "
            + "AND e.matchId IN (SELECT m.id FROM MatchesEntity m WHERE " + ACTIVE_MATCH + ") "
            + "GROUP BY e.playerId")
    List<Object[]> countMatchesWithEventsByPlayer(@Param("teamId") UUID teamId);

    @Query(value = "SELECT m FROM MatchesEntity m WHERE (m.homeTeamId = :teamId OR m.awayTeamId = :teamId) AND "
            + ACTIVE_MATCH + " ORDER BY m.matchDate DESC, m.id",
            countQuery = "SELECT COUNT(m) FROM MatchesEntity m WHERE (m.homeTeamId = :teamId OR m.awayTeamId = :teamId) AND "
                    + ACTIVE_MATCH)
    Page<MatchesEntity> findMatchesOfTeamNewestFirst(@Param("teamId") UUID teamId, Pageable pageable);
}
