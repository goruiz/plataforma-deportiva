package com.platform.backend.shared.infraestructure.tenancy;

import com.platform.backend.shared.domain.tenancy.TenantPrincipal;
import com.platform.backend.shared.domain.tenancy.TenantScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads who is calling from the security context. It is static, like the auditor lookup, because Hibernate (not
 * Spring) builds the objects that need it.
 */
public final class CurrentTenant {

    /** Bound to the filter's email parameter when nobody is calling: no account has an empty email. */
    public static final String NO_EMAIL = "";

    private CurrentTenant() {}

    /**
     * What the caller may see. The platform owner and the code that runs without a user (login, scheduled jobs) see
     * everything; an administrator is restricted to their organization; a player (a mobile-app account) to the
     * organization of their team, plus the public events. A player without a team has no organization and sees only the
     * public events.
     */
    public static TenantScope scope() {
        TenantPrincipal principal = currentPrincipal();
        if (principal == null || principal.isPlatformOwner()) {
            return TenantScope.unrestricted();
        }
        if (principal.isAdministrator()) {
            return principal.getOrganizationId() == null
                    ? TenantScope.unrestricted()
                    : TenantScope.ofOrganization(principal.getOrganizationId());
        }
        return TenantScope.ofPlayer(principal.getOrganizationId());
    }

    public static boolean isPlatformOwner() {
        TenantPrincipal principal = currentPrincipal();
        return principal != null && principal.isPlatformOwner();
    }

    /** The caller's email as it is stored (lower case); {@link #NO_EMAIL} when nobody is calling. */
    public static String callerEmail() {
        TenantPrincipal principal = currentPrincipal();
        if (principal == null || principal.getEmail() == null) {
            return NO_EMAIL;
        }
        return principal.getEmail().toLowerCase();
    }

    private static TenantPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TenantPrincipal principal) {
            return principal;
        }
        return null;
    }
}
