package com.platform.backend.modules.courts.domain.exception;

/**
 * Why a court request was refused. The constant's name is the stable code clients
 * receive as the reason, so they can show it in their own language.
 */
public enum CourtError {
    /** Another active court of the same sport complex already has that name. */
    NAME_ALREADY_EXISTS
}
