package com.platform.backend.modules.players.domain.exception;

/**
 * Why a change to a player was refused. The constant's name is the stable code clients receive as the {@code message} of
 * the 400, so they can show it in their own language.
 */
public enum PlayerError {
    /** Only a player who belongs to a team can be its captain. */
    CAPTAIN_REQUIRES_TEAM,

    /** A player has one team per organization: whoever already has one in the team's organization cannot join another with a code. */
    ALREADY_IN_TEAM,

    /** The player plays for several teams the caller sees and the request did not say which one it is about. */
    TEAM_REQUIRED,

    /** A person shared with another organization cannot be erased by one: it can only take them out of its own team. */
    PLAYS_FOR_ANOTHER_ORGANIZATION
}
