package com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse;

import lombok.Data;

import java.util.UUID;

/** What one player did across all the matches of an event, counted from the timelines of those matches. */
@Data
public class PlayerStatsResponse {

    private UUID playerId;
    /** Null if the player was deleted after playing. */
    private String playerName;
    private UUID teamId;
    private String teamName;
    private int goals;
    private int assists;
    private int yellowCards;
    private int redCards;
    private int fouls;
}
