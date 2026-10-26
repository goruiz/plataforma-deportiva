package com.platform.backend.modules.players.domain.enums;

/**
 * Where a team invitation stands. The column stays free text; expiry is not a status, it is worked out from
 * {@code expires_at}.
 */
public enum InvitationStatus {
    PENDING,
    /** The invited person joined the team (by registering with the token or by accepting the invitation). */
    USED,
    /** The invited person turned it down. */
    DECLINED
}
