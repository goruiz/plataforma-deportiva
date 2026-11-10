package com.platform.backend.modules.matches.domain.models;

import java.util.UUID;

/**
 * What the played matches of one team add up to from one side (all its home matches, or all its away ones). A match is
 * played once it has a result, as in the championship.
 *
 * @param goalsFor     goals the team scored
 * @param goalsAgainst goals the team conceded
 */
public record TeamMatchTally(UUID teamId, long played, long wins, long draws, long losses,
                             long goalsFor, long goalsAgainst) {

    /** The tally of a team with no played match on that side. */
    public static TeamMatchTally empty(UUID teamId) {
        return new TeamMatchTally(teamId, 0, 0, 0, 0, 0, 0);
    }

    /** Both sides of the same team together. */
    public TeamMatchTally plus(TeamMatchTally other) {
        return new TeamMatchTally(teamId, played + other.played, wins + other.wins, draws + other.draws,
                losses + other.losses, goalsFor + other.goalsFor, goalsAgainst + other.goalsAgainst);
    }
}
