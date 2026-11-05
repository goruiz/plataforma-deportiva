package com.platform.backend.modules.events.domain.enums;

import java.util.Arrays;

/**
 * The states an event is shown in. The column stays free text (the panel and the app are the ones that set it), so this
 * only names the values the API understands: to filter and to put what is happening first.
 */
public enum EventStatus {
    ACTIVE,
    UPCOMING,
    FINISHED,
    CANCELLED;

    public static boolean isValid(String status) {
        return Arrays.stream(values()).anyMatch(known -> known.name().equals(status));
    }
}
