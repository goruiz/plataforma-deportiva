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
    PLAYER_NOT_IN_TEAM
}
