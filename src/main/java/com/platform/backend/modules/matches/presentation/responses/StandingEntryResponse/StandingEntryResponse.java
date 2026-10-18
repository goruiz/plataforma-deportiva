package com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse;

import lombok.Data;

import java.util.UUID;

@Data
public class StandingEntryResponse {

    private int position;
    private UUID teamId;
    private String teamName;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
    /** The sum of the points of the teams it has faced (Buchholz), the usual tie-breaker of Swiss rounds. */
    private int opponentsPoints;
    /** In a group stage, the group this line belongs to; null in any other table. */
    private Integer groupNumber;
}
