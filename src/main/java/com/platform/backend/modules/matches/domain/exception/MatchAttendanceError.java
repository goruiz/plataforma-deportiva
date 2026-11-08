package com.platform.backend.modules.matches.domain.exception;

/**
 * Why an answer about attending a match was refused. The constant's name is the stable code clients receive: as the
 * {@code message} of the 400 for the first two, and as the reason of the {@code status} field for the last one.
 */
public enum MatchAttendanceError {
    /** The team of the player is neither the home nor the away team of the match (or the player has no team). */
    NOT_A_PARTICIPANT,

    /** The match already started or ended, or it is cancelled, postponed or suspended: there is nothing to confirm. */
    MATCH_NOT_OPEN,

    /** The status is not one of {@code AttendanceStatus}. */
    INVALID_STATUS
}
