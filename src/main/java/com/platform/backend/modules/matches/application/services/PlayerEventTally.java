package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.domain.enums.MatchEventType;

/** Goals, assists and cards of one player, added up one match event at a time. */
final class PlayerEventTally {

    private int goals;
    private int assists;
    private int yellowCards;
    private int redCards;
    private int fouls;

    /** Counts the event; one whose type is not a known {@link MatchEventType} adds nothing. */
    void add(String eventType) {
        if (!MatchEventType.isValid(eventType)) {
            return;
        }
        switch (MatchEventType.valueOf(eventType)) {
            case GOAL -> goals++;
            case ASSIST -> assists++;
            case YELLOW_CARD -> yellowCards++;
            case RED_CARD -> redCards++;
            case FOUL -> fouls++;
        }
    }

    int goals() { return goals; }
    int assists() { return assists; }
    int yellowCards() { return yellowCards; }
    int redCards() { return redCards; }
    int fouls() { return fouls; }
}
