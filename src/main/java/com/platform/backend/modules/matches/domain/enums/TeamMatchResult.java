package com.platform.backend.modules.matches.domain.enums;

/** How a played match ended for one of its two teams. The constant's name is the stable code clients receive. */
public enum TeamMatchResult {
    WIN,
    DRAW,
    LOSS;

    /** The result of a team that scored {@code goalsFor} and conceded {@code goalsAgainst}. */
    public static TeamMatchResult of(int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) {
            return WIN;
        }
        return goalsFor == goalsAgainst ? DRAW : LOSS;
    }
}
