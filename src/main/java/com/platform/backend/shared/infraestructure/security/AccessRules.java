package com.platform.backend.shared.infraestructure.security;

/**
 * The {@code @PreAuthorize} expressions of the API, in one place. Annotations only take constants, so these repeat the
 * {@link Authority} names: {@code hasRole('X')} matches {@code Authority.X}.
 */
public final class AccessRules {

    /** Platform-wide settings and the customers (organizations) themselves: only the platform owner. */
    public static final String PLATFORM_OWNER_ONLY = "hasRole('PLATFORM_OWNER')";

    /** Managing staff accounts and their roles: any administrator, of the platform or of one organization. */
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private AccessRules() {}
}
