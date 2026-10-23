package com.platform.backend.shared.infraestructure.tenancy;

import com.platform.backend.modules.users.infrastructure.configuration.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Lets a test run as someone in particular: an organization's administrator, the platform owner or nobody. */
public final class TenantTestSupport {

    private TenantTestSupport() {}

    public static void actAsOrganizationAdmin(UUID organizationId) {
        authenticate(AuthenticatedUser.from(
                UUID.randomUUID(), "Ana", null, "Lopez", null, "ana@liga.com", organizationId, true, false));
    }

    public static void actAsPlatformOwner() {
        authenticate(AuthenticatedUser.from(
                UUID.randomUUID(), "Dueño", null, "Plataforma", null, "owner@platform.com", null, true, true));
    }

    /** A mobile-app account that belongs to no organization. */
    public static void actAsMobileAccountWithoutOrganization() {
        authenticate(AuthenticatedUser.from(
                UUID.randomUUID(), "Juan", null, "Perez", null, "juan@email.com", null, false, false));
    }

    public static void actAsNobody() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
