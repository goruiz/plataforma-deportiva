package com.platform.backend.modules.users.domain.enums;

/**
 * The groups the panel lists accounts in. Every account is in exactly one of the last three: the owner role wins over
 * the administrator role, and everyone with neither is a mobile-app user.
 */
public enum UserSection {

    /** Every account, whatever its roles. */
    ALL,

    /** Accounts holding the platform owner's role. */
    OWNERS,

    /** Accounts holding the customers' administrator role and not the owner's. */
    ADMINS,

    /** Accounts holding neither of those roles (the normal state of an account created from the mobile app). */
    APP_USERS
}
