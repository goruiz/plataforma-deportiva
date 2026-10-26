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

    /** A particular mobile-app account, to check what belongs to it and what does not. */
    public static void actAsMobileAccount(UUID id, String email) {
        authenticate(AuthenticatedUser.from(id, "Juan", null, "Perez", null, email, null, false, false));
    }

    /** An administrator of an organization, with the id the records they create are audited with. */
    public static void actAsOrganizationAdmin(UUID id, UUID organizationId) {
        authenticate(AuthenticatedUser.from(id, "Ana", null, "Lopez", null, "ana@liga.com", organizationId, true, false));
    }

    public static void actAsNobody() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
