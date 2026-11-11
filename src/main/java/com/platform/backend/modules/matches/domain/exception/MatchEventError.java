package com.platform.backend.modules.matches.domain.exception;

/**
 * Why a match event (a goal, a card...) was refused. The constant's name is the stable code clients receive as the
 * reason of the offending field.
 */
public enum MatchEventError {
    /** The event type is not one of {@code MatchEventType}. */
    INVALID_EVENT_TYPE,

    /** The team did not play the match. */
    TEAM_NOT_IN_MATCH,

    /** The player does not exist, or is not part of the team. */
    PLAYER_NOT_IN_TEAM,

    /** A substitution has to say which player goes out. */
    REPLACED_PLAYER_REQUIRED,

    /** The player who comes in cannot be the one who goes out. */
    REPLACED_PLAYER_SAME_AS_PLAYER,

    /** The team already made the substitutions the settings allow in a match. */
    SUBSTITUTION_LIMIT_REACHED
}
