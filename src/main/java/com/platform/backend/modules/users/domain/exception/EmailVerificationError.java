package com.platform.backend.modules.users.domain.exception;

/**
 * Why an email could not be confirmed. The constant's name is the stable code clients receive as the reason of the
 * {@code token} field, so they can show it in their own language.
 */
public enum EmailVerificationError {
    /** The link was already used, has expired or never existed (the three look the same on purpose). */
    INVALID_OR_EXPIRED
}
