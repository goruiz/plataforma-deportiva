package com.platform.backend.modules.matches.presentation.responses.PlayerTotalsResponse;

import lombok.Data;

import java.util.UUID;

/**
 * What a player has done in every event they played, the ones of a team they left included: the figures of
 * {@code PlayerStatsResponse} summed over all events, plus the matches played.
 */
@Data
public class PlayerTotalsResponse {

    private UUID playerId;
    /**
     * The played matches (with a result) of the player's current team, plus every other match in which the player has
     * an event, each counted once. The platform does not record when a player joined a team, so a player who joined
     * halfway through a season is credited with the matches their team played before; and a player of a previous team
     * is only credited with the matches where they scored, assisted or were carded.
     */
    private int matchesPlayed;
    private int goals;
    private int assists;
    private int yellowCards;
    private int redCards;
    private int fouls;
}
