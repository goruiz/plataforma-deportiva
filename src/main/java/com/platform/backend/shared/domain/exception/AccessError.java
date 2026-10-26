package com.platform.backend.shared.domain.exception;

/**
 * Why a caller was refused a record they can see but may not change. The constant's name is the stable code clients
 * receive as the {@code message} of the 403.
 */
public enum AccessError {
    /** The record belongs to another account and the caller is not an administrator. */
    NOT_THE_CREATOR
}
