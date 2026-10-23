package com.platform.backend.modules.users.infrastructure.configuration.security;

import com.platform.backend.modules.users.domain.irepositories.IUsersRolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * The platform owner's role ("Propietario"), which is the one in {@code app.security.owner-role-id}. As with the admin
 * role, an account is the owner only if it has an active {@code users_roles} row pointing at that id, never because of
 * a role name. With no owner role configured nobody is the owner.
 * <p>
 * It is independent of the administrator role of the customers: the owner does not hold that one, and counts as an
 * administrator by itself (it can sign in to the panel).
 */
@Component
@RequiredArgsConstructor
public class PlatformOwnerRole {

    private final IUsersRolesRepository usersRolesRepository;

    @Value("${app.security.owner-role-id:}")
    private String ownerRoleId;

    /** The configured id, or empty when no owner role is configured. */
    public Optional<UUID> id() {
        return isConfigured() ? Optional.of(UUID.fromString(ownerRoleId)) : Optional.empty();
    }

    public boolean isOwnerRole(UUID roleId) {
        return isConfigured() && UUID.fromString(ownerRoleId).equals(roleId);
    }

    public boolean isOwner(UUID userId) {
        return isConfigured() && usersRolesRepository.existsActiveByUserAndRole(userId, UUID.fromString(ownerRoleId));
    }

    private boolean isConfigured() {
        return StringUtils.hasText(ownerRoleId);
    }
}
