package com.platform.backend.modules.matches.application.engine;

import com.platform.backend.modules.events.domain.enums.EventFormatStructure;
import com.platform.backend.modules.events.domain.enums.TiebreakerCriterion;
import com.platform.backend.modules.matches.domain.enums.MatchStage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builders shared by the championship engine tests. */
final class EngineFixtures {

    private EngineFixtures() {}

    static List<UUID> teams(int count) {
        List<UUID> teams = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            teams.add(UUID.randomUUID());
        }
        return teams;
    }

    /** A league decided by goal difference and then goals scored, 3 points for a win and 1 for a draw. */
    static ChampionshipRules leagueRules() {
        return ChampionshipRules.legacyLeague();
    }

    static ChampionshipRules rulesWithTiebreakers(TiebreakerCriterion... criteria) {
        return new ChampionshipRules(EventFormatStructure.ROUND_ROBIN, false, 3, 1, 0, List.of(criteria),
                0, 0, 0, 0, false, false, false);
    }

    static ChampionshipRules rulesWithPoints(int win, int draw, int loss) {
        return new ChampionshipRules(EventFormatStructure.ROUND_ROBIN, false, win, draw, loss,
                List.of(TiebreakerCriterion.GOAL_DIFFERENCE, TiebreakerCriterion.GOALS_FOR), 0, 0, 0, 0, false, false, false);
    }

    /** A decided league match. */
    static MatchRef played(UUID home, UUID away, int homeGoals, int awayGoals) {
        return match(home, away, homeGoals, awayGoals, MatchStage.LEAGUE, null, null, null, null);
    }

    static MatchRef match(UUID home, UUID away, Integer homeGoals, Integer awayGoals, MatchStage stage, Integer round,
                          Integer group, Integer tie, Integer leg) {
        return new MatchRef(UUID.randomUUID(), home, away, homeGoals, awayGoals, stage, round, group, tie, leg, null, null, "SCHEDULED");
    }

    static List<Integer> positionsOf(List<StandingRow> table, List<UUID> teams) {
        return teams.stream()
                .map(team -> table.stream().filter(row -> row.teamId().equals(team)).findFirst().orElseThrow().position())
                .toList();
    }

    static List<UUID> orderOf(List<StandingRow> table) {
        return table.stream().map(StandingRow::teamId).toList();
    }
}
