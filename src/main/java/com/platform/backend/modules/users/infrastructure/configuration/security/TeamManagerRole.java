package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * The "Dirigente de equipo" role, which is the one in {@code app.security.team-manager-role-id}. As with the owner and
 * the administrator, an account holds it only if it has an active {@code users_roles} row pointing at that id, never
 * because of a role name. With no id configured nobody is a team manager.
 */
@Component
@RequiredArgsConstructor
public class TeamManagerRole {

    private final IUsersRolesRepository usersRolesRepository;

    @Value("${app.security.team-manager-role-id:}")
    private String teamManagerRoleId;

    public boolean isTeamManager(UUID userId) {
        return StringUtils.hasText(teamManagerRoleId)
                && usersRolesRepository.existsActiveByUserAndRole(userId, UUID.fromString(teamManagerRoleId));
    }
}
