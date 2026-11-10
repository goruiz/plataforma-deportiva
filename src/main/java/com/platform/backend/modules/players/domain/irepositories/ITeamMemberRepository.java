package com.platform.backend.modules.players.domain.irepositories;

import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The teams each player plays for. Every read goes through the organization filter, so an administrator only sees their own. */
public interface ITeamMemberRepository {

    TeamMembersEntity save(TeamMembersEntity membership);

    /** Writes the change right away, for when the order of two writes matters (a unique index). */
    TeamMembersEntity saveAndFlush(TeamMembersEntity membership);

    /** The teams a player plays for, the one joined last first. */
    List<TeamMembersEntity> findAllActiveByPlayerId(UUID playerId);

    /** The same for several players at once, with their teams, so a list is not read one player at a time. */
    List<TeamMembersEntity> findAllActiveByPlayerIds(Collection<UUID> playerIds);

    /** Whoever plays for a team, with their player data. */
    List<TeamMembersEntity> findAllActiveByTeamId(UUID teamId);

    Optional<TeamMembersEntity> findActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId);

    boolean existsActiveByPlayerIdAndTeamId(UUID playerId, UUID teamId);

    /** Whether the player with that email still plays for the team as one of its administrators. */
    boolean existsActiveTeamAdministrator(UUID teamId, String email);

    /**
     * The organization of the team the player joined last, whatever the caller sees: empty when they play for none (or for
     * one of the platform owner's, which has no organization). The account follows it.
     */
    Optional<UUID> findOrganizationIdOfLastJoinedTeam(UUID playerId);

    /**
     * Whether the player also plays for a team of an organization other than the given one, whatever the caller sees:
     * an organization may not erase a person who plays for another.
     */
    boolean existsActiveOutsideOrganization(UUID playerId, UUID organizationId);
}
