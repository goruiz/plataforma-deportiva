package com.platform.backend.modules.matches.domain.exception;

/**
 * Why a match request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum MatchError {
    /** A result can only be recorded once the match is over (kick-off + duration + break). */
    RESULT_BEFORE_MATCH_END,

    /** Once a match has started its data (teams, date, court, place) is fixed: only its status and its result can change. */
    MATCH_ALREADY_STARTED
}
