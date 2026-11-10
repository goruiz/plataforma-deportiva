package com.platform.backend.modules.users.infrastructure.persistence.jpa;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Counts for the users report. A plain {@link Repository} with JPQL only, so the organization filter of each entity
 * applies to every line it reads.
 */
public interface UsersReportJpaRepository extends Repository<UsersEntity, UUID> {

    @Query("SELECT COUNT(u) FROM UsersEntity u WHERE u.deletedAt IS NULL AND u.emailVerifiedAt IS NOT NULL")
    long countAccountsWithVerifiedEmail();

    // Columns: year, month, accounts.
    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) FROM UsersEntity u "
            + "WHERE u.deletedAt IS NULL AND u.createdAt >= :since "
            + "GROUP BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> countAccountsCreatedSinceByMonth(@Param("since") LocalDateTime since);

    @Query("SELECT u.organizationId, COUNT(u) FROM UsersEntity u WHERE u.deletedAt IS NULL GROUP BY u.organizationId")
    List<Object[]> countAccountsByOrganization();

    @Query("SELECT COUNT(DISTINCT m.player.id) FROM TeamMembersEntity m "
            + "WHERE m.deletedAt IS NULL AND m.team.deletedAt IS NULL AND m.player.deletedAt IS NULL")
    long countPlayersWithTeam();

    @Query("SELECT COUNT(p) FROM PlayersEntity p WHERE p.deletedAt IS NULL AND NOT EXISTS "
            + "(SELECT 1 FROM TeamMembersEntity m WHERE m.player.id = p.id AND m.deletedAt IS NULL)")
    long countPlayersWithoutTeam();
}
