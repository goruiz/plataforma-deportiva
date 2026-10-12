package com.platform.backend.modules.teams.domain.enums;

import java.util.Arrays;

/**
 * Values a team's status can take. The column stays free text (the mobile app reads it as such),
 * so {@link #isValid(String)} is how the service checks what a client sent.
 */
public enum TeamStatus {
    ACTIVE,
    INACTIVE;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(status -> status.name().equals(value));
    }
}
