package com.platform.backend.modules.matches.domain.exception;

/**
 * Why a match request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum MatchError {
    /** A result can only be recorded once the match is over (kick-off + duration + break). */
    RESULT_BEFORE_MATCH_END,

    /** Once a match has started its data (teams, date, court, place) is fixed: only its status and its result can change. */
    MATCH_ALREADY_STARTED,

    /** The event the match belongs to does not exist, or belongs to another organization. A new match needs one. */
    EVENT_NOT_FOUND,

    /** A team of the match does not exist, or belongs to another organization. */
    TEAM_NOT_FOUND,

    /** The court of the match does not exist, or belongs to another organization. */
    COURT_NOT_FOUND,

    /** The match a match event is attached to does not exist, or belongs to another organization. */
    MATCH_NOT_FOUND,

    /** The calendar was asked for an event played in complexes that have no court: there is nowhere to schedule the matches. */
    EVENT_COMPLEXES_WITHOUT_COURTS,

    /** The organization's settings do not allow a match that is not an elimination to end level (and no penalties settle it). */
    DRAW_NOT_ALLOWED,

    /** A penalty shoot-out was sent but the organization's settings do not allow penalties. */
    PENALTIES_NOT_ENABLED,

    /** A penalty shoot-out needs both scores (or none). */
    PENALTIES_INCOMPLETE,

    /** A penalty score is negative. */
    INVALID_PENALTY_SCORE,

    /** A penalty shoot-out only exists when the score of the match ended level. */
    PENALTIES_REQUIRE_LEVEL_RESULT,

    /** A penalty shoot-out has a winner: the two penalty scores cannot be equal. */
    PENALTIES_MUST_DECIDE,

    /** The result cannot be validated: the match has no result yet. */
    RESULT_NOT_RECORDED,

    /** The result cannot be validated while a dispute against it is open. */
    HAS_OPEN_DISPUTES
}
