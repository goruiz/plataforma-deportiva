package com.platform.backend.modules.players.application.services;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.players.domain.irepositories.ITeamMemberRepository;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The memberships in a list, for tests of services that put players in teams: the rules (one team per organization, who is
 * captain) run for real over it instead of being stubbed. It does not simulate the organization filter, which only a real
 * database can prove.
 */
class InMemoryTeamMembers implements ITeamMemberRepository {

    private static final UUID NO_ORGANIZATION = new UUID(0L, 0L);

    private final List<TeamMembersEntity> rows = new ArrayList<>();

    /** A player already in a team, as if they had joined before the test. */
    TeamMembersEntity put(PlayersEntity player, TeamsEntity team) {
        return save(TeamMembersEntity.of(player, team));
    }

    /** The teams the player plays for now, the one joined last first. */
    List<TeamsEntity> activeTeamsOf(PlayersEntity player) {
        return findAllActiveByPlayerId(player.getId()).stream().map(TeamMembersEntity::getTeam).toList();
    }

    /** Every membership the player ever had, ended ones included. */
    List<TeamMembersEntity> everyMembershipOf(PlayersEntity player) {
        return rows.stream().filter(row -> row.getPlayer().getId().equals(player.getId())).toList();
    }

    @Override
    public TeamMembersEntity save(TeamMembersEntity membership) {
        if (membership.getId() == null) {
            membership.setId(UUID.randomUUID());
        }
        if (!rows.contains(membership)) {
            rows.add(membership);
        }
        return membership;
    }

    @Override
    public TeamMembersEntity saveAndFlush(TeamMembersEntity membership) {
        return save(membership);
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByPlayerId(UUID playerId) {
        List<TeamMembersEntity> found = new ArrayList<>(rows.stream()
                .filter(row -> row.getDeletedAt() == null && row.getPlayer().getId().equals(playerId))
                .toList());
        java.util.Collections.reverse(found);
        return found;
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByPlayerIds(Collection<UUID> playerIds) {
        return playerIds.stream().flatMap(playerId -> findAllActiveByPlayerId(playerId).stream()).toList();
    }

    @Override
    public List<TeamMembersEntity> findAllActiveByTeamId(UUID teamId) {
        return rows.stream()
                .filter(row -> row.getDeletedAt() == null && row.getTeam().getId().equals(teamId)
                        && row.getPlayer().getDeletedAt() == null)
                .toList();
    }

    @Override
    public Optional<TeamMembersEntity> findActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId) {
        return findAllActiveByPlayerId(playerId).stream().filter(row -> row.getTeam().getId().equals(teamId)).findFirst();
    }

    @Override
    public boolean existsActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId) {
        return findActiveByPlayerIdAndTeamId(playerId, teamId).isPresent();
    }

    @Override
    public boolean existsActiveTeamAdministrator(UUID teamId, String email) {
        return rows.stream().anyMatch(row -> row.getDeletedAt() == null && row.isAdministratorOfTheTeam()
                && row.getTeam().getId().equals(teamId) && email.equalsIgnoreCase(row.getPlayer().getEmail()));
    }

    @Override
    public Optional<UUID> findOrganizationIdOfLastJoinedTeam(UUID playerId) {
        return findAllActiveByPlayerId(playerId).stream().findFirst().map(TeamMembersEntity::getOrganizationId);
    }

    @Override
    public boolean existsActiveOutsideOrganization(UUID playerId, UUID organizationId) {
        return findAllActiveByPlayerId(playerId).stream()
                .anyMatch(row -> !(row.getOrganizationId() == null ? NO_ORGANIZATION : row.getOrganizationId())
                        .equals(organizationId));
    }
}
