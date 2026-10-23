package com.platform.backend.modules.organizations.domain.enums;

import java.util.Arrays;

/**
 * Values an organization's status can take. The column stays free text, so {@link #isValid(String)} is how the
 * service checks what a client sent.
 */
public enum OrganizationStatus {

    /** The organization works normally. */
    ACTIVE,

    /** Its administrators cannot sign in to the admin panel; nothing of its data is touched. */
    SUSPENDED;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(status -> status.name().equals(value));
    }
}
