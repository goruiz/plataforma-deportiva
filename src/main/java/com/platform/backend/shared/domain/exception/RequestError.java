package com.platform.backend.shared.domain.exception;

/**
 * Why the transport of a request was refused before any business rule ran: a query parameter that is missing or does
 * not parse, or an upload over the size limit. The constant's name is the stable code clients receive as the reason of
 * the offending parameter or part, in the {@code data} map of the 400.
 */
public enum RequestError {
    /** A required query parameter was not sent. */
    PARAMETER_REQUIRED,

    /** A query or path parameter was sent in a form that cannot be read (a date that is not {@code yyyy-MM-dd}). */
    PARAMETER_INVALID,

    /** The uploaded file is bigger than the server accepts. */
    FILE_TOO_LARGE
}
