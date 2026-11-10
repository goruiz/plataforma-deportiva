package com.platform.backend.modules.matches.domain.exception;

/**
 * Why a dispute request was refused. The constant's name is the stable code clients receive as the reason (of a
 * field, or as the message of a request the caller may not make), so they can show it in their own language.
 */
public enum DisputeError {
    /** The match has no result yet, so there is nothing to dispute. */
    RESULT_NOT_RECORDED,

    /** The time a team has to dispute a result (the organization's settings) is over. */
    WINDOW_CLOSED,

    /** The team that disputes does not play the match. */
    TEAM_NOT_IN_MATCH,

    /** Only an administrator, or the captain of the team, may dispute on behalf of a team. */
    NOT_ALLOWED_FOR_TEAM,

    /** The team already has a dispute open against this match. */
    ALREADY_OPEN,

    /** The dispute was already resolved. */
    NOT_OPEN,

    /** A dispute needs a reason. */
    REASON_REQUIRED,

    /** Rejecting a dispute needs a note that explains why. */
    NOTE_REQUIRED,

    /** A score has to come with its pair (both goals, or none). */
    SCORE_INCOMPLETE,

    /** A score cannot be negative. */
    INVALID_SCORE,

    /** The status asked for in a list is not OPEN, ACCEPTED or REJECTED. */
    INVALID_STATUS
}
