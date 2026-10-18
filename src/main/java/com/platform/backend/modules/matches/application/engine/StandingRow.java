package com.platform.backend.modules.matches.application.engine;

import java.util.UUID;

/** One line of a points table. */
public record StandingRow(UUID teamId, int position, int played, int won, int drawn, int lost,
                          int goalsFor, int goalsAgainst, int points, int opponentsPoints) {

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}
