package com.platform.backend.modules.users.domain.exception;

/**
 * Why an account could not be deleted. The constant's name is the stable code clients receive: as the reason of the
 * {@code password} field for {@link #INCORRECT_PASSWORD}, and as the {@code message} of the 400 for the other.
 */
public enum AccountDeletionError {
    /** The password sent to confirm the deletion is not the account's. */
    INCORRECT_PASSWORD,

    /** An administrator or the platform owner works for an organization: their accounts are removed from the panel. */
    STAFF_ACCOUNT
}
