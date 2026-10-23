package com.platform.backend.modules.users.domain.exception;

/**
 * Why a login with valid credentials was refused (always a 403). The constant's name is the stable code clients
 * receive as the message, so they can tell the cases apart and show each one in their own language.
 */
public enum LoginError {
    /** The account is not an administrator and tried the admin panel. */
    ADMIN_ACCESS_REQUIRED,

    /** An administrator tried the mobile app. */
    ADMIN_CANNOT_USE_MOBILE_APP,

    /** An administrator who is not the platform owner belongs to no organization. */
    ORGANIZATION_REQUIRED,

    /** The account's organization no longer exists. */
    ORGANIZATION_NOT_FOUND,

    /** The account's organization is suspended. */
    ORGANIZATION_SUSPENDED
}
