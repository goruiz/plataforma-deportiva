package com.platform.backend.modules.matches.presentation.responses.TeamStatsResponse;

import lombok.Data;

import java.util.UUID;

/**
 * What a team has done across every event: its played matches (the ones with a result) and what the timelines of
 * those matches say about its cards and fouls.
 */
@Data
public class TeamStatsResponse {

    private UUID teamId;
    private String teamName;
    /** The customer the team belongs to; null for the platform owner's. */
    private UUID organizationId;
    /** In how many events the team is enrolled, played or not yet. */
    private int eventsEnrolled;
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int yellowCards;
    private int redCards;
    private int fouls;
}
