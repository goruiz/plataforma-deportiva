package com.platform.backend.modules.users.domain.exception;

/**
 * Why an account could not be suspended or reactivated. The constant's name is the stable code clients receive as the
 * {@code message} of the refusal, so they can show it in their own language.
 */
public enum AccountSuspensionError {
    /** Whoever suspends an account must not lock themselves out: another administrator has to do it. */
    CANNOT_SUSPEND_YOURSELF,

    /** Only the platform owner may suspend (or reactivate) the account of another platform owner. */
    PLATFORM_OWNER_PROTECTED
}
