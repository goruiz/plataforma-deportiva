package com.platform.backend.shared.domain.security;

import java.util.UUID;

/**
 * Whether an account administers a team. The players module knows who administers which team and implements this, so
 * the modules that only need to ask (teams) do not have to know how the memberships are stored.
 */
public interface TeamAdministratorChecker {

    /** True when the account with that email is one of the administrators of the team (and still plays for it). */
    boolean isTeamAdministrator(UUID teamId, String email);
}
