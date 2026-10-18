package com.platform.backend.modules.events.domain.enums;

/**
 * One stage of a championship. The {@link EventFormatStructure} of an event's format is a sequence of these
 * (a league and then playoffs, groups and then knockout rounds...), played in order: a phase starts when the
 * previous one has finished.
 */
public enum ChampionshipPhase {
    /** Everyone plays everyone; one points table. */
    LEAGUE,
    /** The teams are drawn into groups; everyone plays their group; one points table per group. */
    GROUPS,
    /** A fixed number of rounds, each pairing teams with similar points; one points table. */
    SWISS,
    /** Single elimination: the loser of each tie is out. */
    KNOCKOUT,
    /** A team is out after its second defeat. */
    DOUBLE_ELIMINATION;

    /** Phases that rank teams on a points table. */
    public boolean hasPointsTable() {
        return this == LEAGUE || this == GROUPS || this == SWISS;
    }

    /** Phases whose rounds are decided by ties that eliminate teams. */
    public boolean isElimination() {
        return this == KNOCKOUT || this == DOUBLE_ELIMINATION;
    }
}
