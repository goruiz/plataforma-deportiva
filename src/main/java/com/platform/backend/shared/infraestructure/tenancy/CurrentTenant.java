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

    private CurrentTenant() {}

    /**
     * What the caller may see. A caller restricted to one organization is an administrator of that organization; the
     * platform owner is not. Anyone else is unrestricted, which today means the mobile-app accounts (their access to
     * several organizations is still to be decided) and the code that runs without a user (login, scheduled jobs).
     */
    public static TenantScope scope() {
        TenantPrincipal principal = currentPrincipal();
        if (principal == null || principal.isPlatformOwner() || principal.getOrganizationId() == null) {
            return TenantScope.unrestricted();
        }
        return TenantScope.ofOrganization(principal.getOrganizationId());
    }

    public static boolean isPlatformOwner() {
        TenantPrincipal principal = currentPrincipal();
        return principal != null && principal.isPlatformOwner();
    }

    private static TenantPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TenantPrincipal principal) {
            return principal;
        }
        return null;
    }
}
