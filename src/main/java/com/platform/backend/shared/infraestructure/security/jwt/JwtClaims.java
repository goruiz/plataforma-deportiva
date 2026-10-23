package com.platform.backend.shared.infraestructure.security.jwt;

/** Names of the claims the login puts in the JWT beyond the user's id and name. */
public final class JwtClaims {

    public static final String ADMIN = "admin";
    public static final String PLATFORM_OWNER = "platformOwner";
    public static final String ORGANIZATION_ID = "organizationId";

    private JwtClaims() {}
}
