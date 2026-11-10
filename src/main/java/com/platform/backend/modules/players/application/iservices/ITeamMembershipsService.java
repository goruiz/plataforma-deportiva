package com.platform.backend.modules.players.application.iservices;

import com.platform.backend.modules.players.domain.entities.PlayersEntity;
import com.platform.backend.modules.players.domain.entities.TeamMembersEntity;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;

/**
 * Who plays for which team. A player can play for several teams, but only one per organization: joining a team of an
 * organization where the player already has one replaces it, and a team of another organization is simply added.
 */
public interface ITeamMembershipsService {

    /**
     * Puts the player in the team. Only the team they have in the team's organization is left (a captain stops being one
     * with it): the teams of other organizations are untouched. Joining the team they already play for changes nothing.
     */
    TeamMembersEntity join(PlayersEntity player, TeamsEntity team);

    /** The player stops playing for that team; the others stay. */
    void leave(TeamMembersEntity membership);

    /** The player stops playing for every team the caller can see them in (all of them when it is the player themselves). */
    void leaveEvery(PlayersEntity player);
}
