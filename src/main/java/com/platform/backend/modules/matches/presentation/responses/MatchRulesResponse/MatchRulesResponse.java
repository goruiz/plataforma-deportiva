package com.platform.backend.modules.matches.presentation.responses.MatchRulesResponse;

import lombok.Data;

/**
 * The match rules the matches of one event follow: those of the organization the event belongs to, which are not
 * always those of whoever asks (the platform owner manages events of every organization).
 */
@Data
public class MatchRulesResponse {

    /** Whether a match that is not an elimination may end level. */
    private boolean drawsAllowed;
    /** Whether a level result may be settled with a penalty shoot-out. */
    private boolean penaltiesEnabled;
    private int extraTimeMinutes;
    /** How long after a result is recorded a team may dispute it. */
    private int disputeWindowHours;
}
