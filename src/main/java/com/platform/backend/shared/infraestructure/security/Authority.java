package com.platform.backend.shared.infraestructure.security;

/**
 * What an authenticated caller is allowed to be, as Spring Security authorities. They come from the JWT claims issued
 * at login, never from role names (those are editable).
 */
public enum Authority {

    /** Every authenticated account. */
    USER,

    /** Can use the admin panel: the platform owner or an administrator of one organization. */
    ADMIN,

    /** The platform owner: sees every organization and manages the platform itself. */
    PLATFORM_OWNER;

    /** The name Spring Security expects ({@code hasRole('ADMIN')} looks for {@code ROLE_ADMIN}). */
    public String roleName() {
        return "ROLE_" + name();
    }
}
