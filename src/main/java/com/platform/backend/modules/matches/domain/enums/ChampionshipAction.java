package com.platform.backend.modules.matches.domain.enums;

/** What the administrator should do next to move a championship forward. The name is a stable code clients translate. */
public enum ChampionshipAction {
    /** The event has no format, so there is no championship to run. */
    NO_FORMAT,
    /** Fewer teams are registered than the format needs to start. */
    NOT_ENOUGH_TEAMS,
    /** Create the whole calendar of a league or of the group stage. */
    GENERATE_CALENDAR,
    /** Create the next round (Swiss, knockout or double elimination). */
    GENERATE_ROUND,
    /** The matches already generated still have no result. */
    WAITING_RESULTS,
    /** A tie ended level and an administrator must say which team advances. */
    NEEDS_TIEBREAK,
    /** Everything has been played. */
    FINISHED
}
