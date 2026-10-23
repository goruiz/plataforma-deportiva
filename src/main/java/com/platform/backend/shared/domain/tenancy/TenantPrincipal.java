package com.platform.backend.shared.domain.tenancy;

import java.util.UUID;

/**
 * Who is calling, as far as organization scoping is concerned. The authenticated user implements it, so the
 * tenancy code in {@code shared} does not have to know the users module.
 */
public interface TenantPrincipal {

    /** The organization the caller works for; null for the platform owner and for mobile-app accounts. */
    UUID getOrganizationId();

    /** The platform owner (the system's seller) sees every organization. */
    boolean isPlatformOwner();
}
