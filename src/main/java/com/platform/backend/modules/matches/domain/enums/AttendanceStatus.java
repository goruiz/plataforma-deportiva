package com.platform.backend.modules.matches.domain.enums;

import java.util.Arrays;

/**
 * What a player answers when asked whether they will be at a match. The column {@code match_attendances.status} is
 * stored by name, as text, like every state of this project: {@link #isValid(String)} is how the service checks what a
 * client sent. No answer at all is not a status: the player simply has no row.
 */
public enum AttendanceStatus {
    CONFIRMED,
    DECLINED;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(status -> status.name().equals(value));
    }
}
