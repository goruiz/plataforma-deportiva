package com.platform.backend.shared.domain.security;

import java.time.Instant;
import java.util.UUID;

/**
 * Whether a token that is cryptographically valid still opens a session. The JWT is stateless, so on its own it would
 * keep working until it expires even after its owner signed out or deleted the account; the users module implements
 * this to say no in those cases, and the JWT filter asks it, so {@code shared} does not have to know how.
 */
public interface SessionChecker {

    /** True when the account exists, is not deleted or suspended and has not signed out since {@code issuedAt}. */
    boolean isSessionValid(UUID userId, Instant issuedAt);
}
