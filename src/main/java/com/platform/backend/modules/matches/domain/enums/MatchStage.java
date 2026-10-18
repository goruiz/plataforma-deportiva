package com.platform.backend.modules.matches.domain.enums;

import com.platform.backend.modules.events.domain.enums.ChampionshipPhase;

import java.util.Arrays;
import java.util.List;

/**
 * The part of the championship a match belongs to. Stored by name as plain text (like every state of this project, so
 * a new one never needs the database to be altered); a match without one was created by hand or before championships
 * existed, and counts as a league match.
 */
public enum MatchStage {
    LEAGUE(ChampionshipPhase.LEAGUE),
    GROUP(ChampionshipPhase.GROUPS),
    SWISS(ChampionshipPhase.SWISS),
    KNOCKOUT(ChampionshipPhase.KNOCKOUT),
    /** The match between the two losing semi-finalists. */
    THIRD_PLACE(ChampionshipPhase.KNOCKOUT),
    /** Double elimination: teams that have not lost yet. */
    WINNERS(ChampionshipPhase.DOUBLE_ELIMINATION),
    /** Double elimination: teams with one defeat. */
    LOSERS(ChampionshipPhase.DOUBLE_ELIMINATION),
    /** Double elimination: the last undefeated team against the last team with one defeat. */
    GRAND_FINAL(ChampionshipPhase.DOUBLE_ELIMINATION);

    private final ChampionshipPhase phase;

    MatchStage(ChampionshipPhase phase) {
        this.phase = phase;
    }

    public ChampionshipPhase phase() { return phase; }

    /** The stage kept in the column; null (a match with none) is a league match. */
    public static MatchStage fromStored(String stored) {
        return stored == null ? LEAGUE : valueOf(stored);
    }

    /** The stages that make up a phase. */
    public static List<MatchStage> of(ChampionshipPhase phase) {
        return Arrays.stream(values()).filter(stage -> stage.phase == phase).toList();
    }
}
