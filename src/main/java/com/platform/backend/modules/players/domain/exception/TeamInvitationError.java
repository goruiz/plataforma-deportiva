package com.platform.backend.modules.players.domain.exception;

/**
 * Why an invitation could not be accepted, declined or used to register. The constant's name is the stable code clients
 * receive as the {@code message} of the 400 (404 for {@link #INVITATION_NOT_FOUND}), so they can show it in their own
 * language.
 */
public enum TeamInvitationError {
    /** It was already accepted, declined or used to register. */
    NOT_PENDING,

    /** It expired before it was answered. */
    EXPIRED,

    /** The team that invited no longer exists. */
    TEAM_NOT_FOUND,

    /** The invited email has no player to put in the team: they have to register first. */
    PLAYER_NOT_FOUND,

    /** The token of a registration link matches no invitation. */
    INVITATION_NOT_FOUND
}
