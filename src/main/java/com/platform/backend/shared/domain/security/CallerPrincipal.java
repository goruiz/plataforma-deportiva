package com.platform.backend.shared.domain.security;

import java.util.UUID;

/**
 * Who is calling, as far as "who is this account" is concerned. The authenticated user implements it, so the access
 * code in {@code shared} (and the modules that only need to know the caller) do not have to know the users module.
 */
public interface CallerPrincipal {

    /** The id of the caller's account, the same one audited as {@code createdBy} on everything they create. */
    UUID getId();

    /** The email of the caller's account (the one used to log in), which is also how a player is matched to it. */
    String getEmail();
}
