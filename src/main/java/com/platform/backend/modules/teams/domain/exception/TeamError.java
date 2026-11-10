package com.platform.backend.modules.teams.domain.exception;

/**
 * Why a team request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum TeamError {
    NAME_ALREADY_EXISTS,
    CATEGORY_NOT_FOUND,
    INVALID_STATUS,

    /** No active team has that join code (a mistyped one, one that was replaced or switched off look the same). */
    JOIN_CODE_NOT_FOUND,

    /**
     * The team is inactive: it receives nothing new (no enrollment in an event, no invitation, no join code and no
     * player). What it already has (enrollments, matches, players) stays as it is.
     */
    TEAM_INACTIVE
}
