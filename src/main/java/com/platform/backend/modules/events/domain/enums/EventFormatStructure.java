package com.platform.backend.modules.events.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.platform.backend.modules.events.domain.enums.ChampionshipPhase.*;
import static com.platform.backend.modules.events.domain.enums.EventFormatRule.*;

/**
 * How an event is played. Each structure declares the phases a championship goes through and the rules that mean
 * something for it (see {@link EventFormatRule}): a format of that structure must have all of them and none of the others.
 */
public enum EventFormatStructure {
    /** Everyone plays everyone; the points table decides. */
    ROUND_ROBIN(List.of(LEAGUE),
            ROUND_TRIP, POINTS_FOR_WIN, POINTS_FOR_DRAW, POINTS_FOR_LOSS, TIEBREAKERS),
    /** A fixed number of rounds in which teams with similar points meet, never twice. */
    SWISS(List.of(ChampionshipPhase.SWISS),
            SWISS_ROUNDS, POINTS_FOR_WIN, POINTS_FOR_DRAW, POINTS_FOR_LOSS, TIEBREAKERS),
    /** One defeat and the team is out. */
    KNOCKOUT(List.of(ChampionshipPhase.KNOCKOUT),
            KNOCKOUT_ROUND_TRIP, THIRD_PLACE_MATCH),
    /** A team is out after two defeats; the winners' and losers' sides meet in the grand final. */
    DOUBLE_ELIMINATION(List.of(ChampionshipPhase.DOUBLE_ELIMINATION),
            KNOCKOUT_ROUND_TRIP, GRAND_FINAL_RESET),
    /** Group stage with a points table per group, then knockout rounds. */
    GROUPS_KNOCKOUT(List.of(GROUPS, ChampionshipPhase.KNOCKOUT),
            ROUND_TRIP, POINTS_FOR_WIN, POINTS_FOR_DRAW, POINTS_FOR_LOSS, TIEBREAKERS,
            GROUP_COUNT, TEAMS_ADVANCING_PER_GROUP, KNOCKOUT_ROUND_TRIP, THIRD_PLACE_MATCH),
    /** A league (regular season), then the best teams play knockout rounds (playoffs). */
    LEAGUE_KNOCKOUT(List.of(LEAGUE, ChampionshipPhase.KNOCKOUT),
            ROUND_TRIP, POINTS_FOR_WIN, POINTS_FOR_DRAW, POINTS_FOR_LOSS, TIEBREAKERS,
            PLAYOFF_TEAMS, KNOCKOUT_ROUND_TRIP, THIRD_PLACE_MATCH),
    /** Swiss rounds, then the best teams play knockout rounds (playoffs). */
    SWISS_KNOCKOUT(List.of(ChampionshipPhase.SWISS, ChampionshipPhase.KNOCKOUT),
            SWISS_ROUNDS, POINTS_FOR_WIN, POINTS_FOR_DRAW, POINTS_FOR_LOSS, TIEBREAKERS,
            PLAYOFF_TEAMS, KNOCKOUT_ROUND_TRIP, THIRD_PLACE_MATCH);

    private final List<ChampionshipPhase> phases;
    private final Set<EventFormatRule> rules;

    EventFormatStructure(List<ChampionshipPhase> phases, EventFormatRule... rules) {
        this.phases = phases;
        this.rules = Collections.unmodifiableSet(EnumSet.of(rules[0], rules));
    }

    /** The structure kept in the column (stored by name); null when the format has none yet. */
    public static EventFormatStructure fromStored(String stored) {
        return stored == null ? null : valueOf(stored);
    }

    /** The phases of a championship of this structure, in the order they are played. */
    public List<ChampionshipPhase> phases() { return phases; }

    /** The rules this structure uses. */
    public Set<EventFormatRule> rules() { return rules; }

    public boolean uses(EventFormatRule rule) { return rules.contains(rule); }
}
