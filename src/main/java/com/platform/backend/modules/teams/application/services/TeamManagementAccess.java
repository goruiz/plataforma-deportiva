package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.shared.domain.exception.AccessError;
import com.platform.backend.shared.domain.security.TeamAdministratorChecker;
import com.platform.backend.shared.infraestructure.security.CreatorAccess;
import com.platform.backend.shared.infraestructure.security.CurrentCaller;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Who may manage what a team shares with its players (its join code): an administrator of the platform, the account
 * that created the team, and the players its administrators named. That last door only opens for their own team: being
 * an administrator of one team gives nothing over another.
 */
@Component
@RequiredArgsConstructor
public class TeamManagementAccess {

    private final TeamAdministratorChecker teamAdministratorChecker;

    public void requireCanManage(TeamsEntity team) {
        if (CreatorAccess.isAdminOrCreator(team.getCreatedBy()) || isAdministratorOf(team)) {
            return;
        }
        throw new AccessDeniedException(AccessError.NOT_THE_CREATOR.name());
    }

    private boolean isAdministratorOf(TeamsEntity team) {
        return teamAdministratorChecker.isTeamAdministrator(team.getId(), CurrentCaller.require().getEmail());
    }
}
