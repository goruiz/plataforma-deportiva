package com.platform.backend.modules.matches.domain.exception;

/**
 * Why a championship request was refused, besides the reasons that are a {@code ChampionshipAction} (for example
 * {@code WAITING_RESULTS}). The constant's name is the stable code clients receive, so they can show it in their language.
 */
public enum ChampionshipError {
    /** The calendar configuration (play days, kick-off time, durations) has to be saved before matches can be scheduled. */
    SCHEDULE_CONFIG_REQUIRED,
    GROUPS_NOT_DRAWN,
    /** The groups cannot be drawn again once their matches exist. */
    GROUPS_ALREADY_GENERATED,
    /** The format has no group stage, so there are no groups to draw. */
    FORMAT_HAS_NO_GROUPS,
    NOT_AN_ELIMINATION_MATCH,
    /** Only a tie that ended level on goals needs someone to say which team advances. */
    TIE_NOT_LEVEL,
    /** The team that advances must be one of the two teams of the tie. */
    TEAM_NOT_IN_TIE,
    /** The advancing team is set on the match that closes the tie (the return leg), not on the first leg. */
    NOT_THE_DECIDING_MATCH
}
