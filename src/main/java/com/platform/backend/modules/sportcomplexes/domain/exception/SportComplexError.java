package com.platform.backend.modules.sportcomplexes.domain.exception;

/**
 * Why a sport complex request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum SportComplexError {
    NAME_ALREADY_EXISTS,
    /** Latitude and longitude go together: one without the other places the complex nowhere. */
    COORDINATES_INCOMPLETE,
    /** The complex still has active courts. */
    IN_USE
}
