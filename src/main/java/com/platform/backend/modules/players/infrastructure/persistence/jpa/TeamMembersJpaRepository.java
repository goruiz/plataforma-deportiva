package com.platform.backend.modules.players.infrastructure.persistence.jpa;

import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.shared.infraestructure.tenancy.TenantScopedJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMembersJpaRepository extends TenantScopedJpaRepository<TeamMembersEntity> {

    @Query("SELECT m FROM TeamMembersEntity m JOIN FETCH m.team "
            + "WHERE m.player.id = :playerId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<TeamMembersEntity> findAllActiveByPlayerId(@Param("playerId") UUID playerId);

    @Query("SELECT m FROM TeamMembersEntity m JOIN FETCH m.team "
            + "WHERE m.player.id IN :playerIds AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<TeamMembersEntity> findAllActiveByPlayerIds(@Param("playerIds") Collection<UUID> playerIds);

    @Query("SELECT m FROM TeamMembersEntity m JOIN FETCH m.player JOIN FETCH m.team "
            + "WHERE m.team.id = :teamId AND m.deletedAt IS NULL AND m.player.deletedAt IS NULL")
    List<TeamMembersEntity> findAllActiveByTeamId(@Param("teamId") UUID teamId);

    @Query("SELECT m FROM TeamMembersEntity m JOIN FETCH m.team "
            + "WHERE m.player.id = :playerId AND m.team.id = :teamId AND m.deletedAt IS NULL")
    Optional<TeamMembersEntity> findActiveByPlayerIdAndTeamId(@Param("playerId") UUID playerId, @Param("teamId") UUID teamId);

    @Query("SELECT COUNT(m) > 0 FROM TeamMembersEntity m "
            + "WHERE m.player.id = :playerId AND m.team.id = :teamId AND m.deletedAt IS NULL")
    boolean existsActiveByPlayerIdAndTeamId(@Param("playerId") UUID playerId, @Param("teamId") UUID teamId);

    // Native on purpose, like the next one: the account of a player follows their last team, and whoever removes them from
    // a team (an administrator) does not see the teams they have in other organizations.
    @Query(value = "SELECT organization_id FROM team_members WHERE player_id = :playerId AND deleted_at IS NULL "
            + "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<UUID> findOrganizationIdOfLastJoinedTeam(@Param("playerId") UUID playerId);

    // Native on purpose: who administers a team is asked on behalf of its own players (mobile accounts), and only the
    // fact is read. The email is the player's, which is the one of the account.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM team_members member JOIN players player ON player.id = member.player_id "
            + "WHERE member.team_id = :teamId AND member.is_team_admin = true AND member.deleted_at IS NULL "
            + "AND player.deleted_at IS NULL AND player.email = :email)", nativeQuery = true)
    boolean existsActiveTeamAdministrator(@Param("teamId") UUID teamId, @Param("email") String email);

    // Native on purpose: it has to see the memberships of the other organizations, which the organization filter hides.
    // Only the fact is read, never which team. The all-zero id stands for the owner's organization (a null one).
    @Query(value = "SELECT EXISTS (SELECT 1 FROM team_members WHERE player_id = :playerId AND deleted_at IS NULL "
            + "AND COALESCE(organization_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid)) <> :organizationId)",
            nativeQuery = true)
    boolean existsActiveOutsideOrganization(@Param("playerId") UUID playerId, @Param("organizationId") UUID organizationId);
}
