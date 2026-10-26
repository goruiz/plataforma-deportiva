package com.platform.backend.modules.matches.domain.enums;

import java.util.Arrays;

/**
 * What a player did in a match (a line of its timeline). The column {@code match_events.event_type} stays free text,
 * like every state of this project, so {@link #isValid(String)} is how the service checks what a client sent.
 */
public enum MatchEventType {
    GOAL,
    ASSIST,
    YELLOW_CARD,
    RED_CARD,
    FOUL;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(type -> type.name().equals(value));
    }
}
