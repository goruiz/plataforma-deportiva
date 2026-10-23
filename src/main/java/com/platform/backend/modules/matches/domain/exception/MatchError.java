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
    MATCH_NOT_FOUND
}
