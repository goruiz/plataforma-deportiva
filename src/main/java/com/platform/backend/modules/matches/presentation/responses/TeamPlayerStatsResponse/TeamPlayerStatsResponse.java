package com.platform.backend.modules.matches.presentation.responses.TeamPlayerStatsResponse;

import lombok.Data;

import java.util.UUID;

/** What one player did with one team, counted from the timelines of the team's matches. */
@Data
public class TeamPlayerStatsResponse {

    private UUID playerId;
    /** Null if the player was deleted after playing. */
    private String playerName;
    /** False for someone who played for the team and left it: they stay in the ranking because their events do. */
    private boolean onRoster;
    /**
     * The played matches of the team (with a result) for someone on its roster, as the platform does not record when a
     * player joined; for someone who left it, only the matches where they have an event.
     */
    private int matchesPlayed;
    private int goals;
    private int assists;
    private int yellowCards;
    private int redCards;
    private int fouls;
}
