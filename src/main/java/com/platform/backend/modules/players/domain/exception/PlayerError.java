package com.platform.backend.modules.players.domain.exception;

/**
 * Why a change to a player was refused. The constant's name is the stable code clients receive as the {@code message} of
 * the 400, so they can show it in their own language.
 */
public enum PlayerError {
    /** Only a player who belongs to a team can be its captain. */
    CAPTAIN_REQUIRES_TEAM
}
