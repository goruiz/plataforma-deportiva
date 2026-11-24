package com.platform.backend.modules.users.infrastructure.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * The customers' administrator role ("Administrador"), which is the one in {@code app.security.admin-role-id}. A role is
 * the administrator's only by that id, never by its name, which is editable. With no id configured no role is.
 * <p>
 * It is independent of {@link PlatformOwnerRole}: the owner does not hold this one.
 */
@Component
public class AdministratorRole {

    @Value("${app.security.admin-role-id:}")
    private String administratorRoleId;

    public boolean isAdministratorRole(UUID roleId) {
        return StringUtils.hasText(administratorRoleId) && UUID.fromString(administratorRoleId).equals(roleId);
    }
}
