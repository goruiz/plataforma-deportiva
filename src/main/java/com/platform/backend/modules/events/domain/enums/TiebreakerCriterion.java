package com.platform.backend.modules.events.domain.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** What separates two teams that finish level on points. A format lists them in priority order. */
public enum TiebreakerCriterion {
    GOAL_DIFFERENCE,
    GOALS_FOR,
    WINS,
    HEAD_TO_HEAD,
    /** The sum of the points of the teams it has faced (Buchholz), the usual tie-breaker of Swiss rounds. */
    OPPONENTS_POINTS;

    private static final String STORED_SEPARATOR = ",";

    /** The stored text ("GOAL_DIFFERENCE,GOALS_FOR") as a list in priority order; null when nothing was stored. */
    public static List<TiebreakerCriterion> fromStored(String stored) {
        if (stored == null) {
            return null;
        }
        if (stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split(STORED_SEPARATOR))
                .map(String::trim)
                .map(TiebreakerCriterion::valueOf)
                .toList();
    }

    /** The text kept in the column for a list; null stays null so "no rule yet" is told apart from "no tie-breakers". */
    public static String toStored(List<TiebreakerCriterion> criteria) {
        if (criteria == null) {
            return null;
        }
        return criteria.stream().map(Enum::name).collect(Collectors.joining(STORED_SEPARATOR));
    }
}
