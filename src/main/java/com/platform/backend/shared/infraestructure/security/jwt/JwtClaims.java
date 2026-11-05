package com.platform.backend.shared.infraestructure.security.jwt;

/** Names of the claims the login puts in the JWT beyond the user's id and name. */
public final class JwtClaims {

    public static final String ADMIN = "admin";
    public static final String PLATFORM_OWNER = "platformOwner";
    public static final String ORGANIZATION_ID = "organizationId";

    /**
     * The moment the token was issued, in epoch milliseconds. The standard {@code iat} only has whole seconds, which is
     * too coarse to tell whether a token was issued before or after a sign-out that happened in the same second.
     */
    public static final String ISSUED_AT_MILLIS = "issuedAtMillis";

    private JwtClaims() {}
}
