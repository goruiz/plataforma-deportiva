package com.platform.backend.shared.domain.exception;

/**
 * Why a caller was refused something they can see but may not do. The constant's name is the stable code clients
 * receive as the {@code message} of the 403.
 */
public enum AccessError {
    /** The record belongs to another account and the caller is not an administrator. */
    NOT_THE_CREATOR,

    /** The account has not confirmed its email, so it cannot do what depends on that (join a team). */
    EMAIL_NOT_VERIFIED
}
