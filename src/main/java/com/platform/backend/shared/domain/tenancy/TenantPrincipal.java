package com.platform.backend.shared.domain.tenancy;

import java.util.UUID;

/**
 * Who is calling, as far as organization scoping is concerned. The authenticated user implements it, so the
 * tenancy code in {@code shared} does not have to know the users module.
 */
public interface TenantPrincipal {

    /**
     * The organization the caller works for or, for a mobile-app account, plays for (the one of its team); null for the
     * platform owner and for an account that belongs to none.
     */
    UUID getOrganizationId();

    /** The platform owner (the system's seller) sees every organization. */
    boolean isPlatformOwner();

    /** An administrator works for an organization; whoever is neither an administrator nor the owner is a player. */
    boolean isAdministrator();

    /** The email of the caller's account: whoever calls always sees their own account, player and invitations. */
    String getEmail();
}
