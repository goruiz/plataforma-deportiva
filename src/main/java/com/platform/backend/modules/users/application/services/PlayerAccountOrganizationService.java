package com.platform.backend.modules.users.application.services;

import com.platform.backend.modules.users.application.iservices.IPlayerAccountOrganizationService;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.configuration.security.PlatformOwnerRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerAccountOrganizationService implements IPlayerAccountOrganizationService {

    /** The role list of the update cannot be empty, and no role has this id. */
    private static final UUID NO_ROLE = new UUID(0L, 0L);

    private final IUserRepository userRepository;
    private final PlatformOwnerRole platformOwnerRole;

    @Value("${app.security.admin-role-id:}")
    private String adminRoleId;

    @Override
    @Transactional
    public void followTeam(String email, UUID teamOrganizationId) {
        if (email == null) {
            return;
        }
        if (teamOrganizationId == null) {
            userRepository.clearOrganizationOfPlayerAccount(email, privilegedRoleIds());
            return;
        }
        userRepository.assignOrganizationToPlayerAccount(email, teamOrganizationId, privilegedRoleIds());
    }

    /** Who works for an organization (or owns the platform) is not a player: their organization is not their team's. */
    private List<UUID> privilegedRoleIds() {
        List<UUID> roleIds = new ArrayList<>();
        if (StringUtils.hasText(adminRoleId)) {
            roleIds.add(UUID.fromString(adminRoleId));
        }
        platformOwnerRole.id().ifPresent(roleIds::add);
        return roleIds.isEmpty() ? List.of(NO_ROLE) : roleIds;
    }
}
