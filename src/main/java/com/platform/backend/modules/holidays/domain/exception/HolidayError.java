package com.platform.backend.modules.holidays.domain.exception;

/**
 * Why a holiday request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum HolidayError {
    /** Another holiday already covers one of those days (a recurring one counts every year). */
    DATE_ALREADY_EXISTS,
    /** The last day of the break comes before its first day. */
    END_DATE_BEFORE_DATE,
    /** The break lasts longer than a year. */
    RANGE_TOO_LONG
}
