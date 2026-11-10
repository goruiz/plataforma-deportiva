package com.platform.backend.modules.matches.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Where a dispute against a match result stands. Stored by name as plain text, like every state of this project, so a
 * new one never needs the database to be altered.
 */
public enum DisputeStatus {
    /** Waiting for an administrator to decide. */
    OPEN,
    /** The administrator upheld the complaint (and corrected the result, when they had a different one). */
    ACCEPTED,
    /** The administrator kept the result as it was. */
    REJECTED;

    /** The status written as text (any case), or empty when it is not one of them. */
    public static Optional<DisputeStatus> parse(String value) {
        return value == null ? Optional.empty()
                : Arrays.stream(values()).filter(status -> status.name().equalsIgnoreCase(value.trim())).findFirst();
    }
}
