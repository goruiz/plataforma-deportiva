package com.platform.backend.modules.matches.domain.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The statuses a match can have. The column is still free text (the mobile app writes some of these values
 * itself), so this enum names them and answers questions about them; it is not what validates the column.
 */
public enum MatchStatus {
    SCHEDULED,
    IN_PROGRESS,
    FINISHED,
    COMPLETED,
    CANCELLED,
    POSTPONED,
    SUSPENDED,
    RESCHEDULED;

    // Not played yet: still to be (re)scheduled. Anything else was played, is being played or was called off for good.
    private static final Set<MatchStatus> PENDING = EnumSet.of(SCHEDULED, POSTPONED, SUSPENDED, RESCHEDULED);

    // Matches that are meant to be played (or being played): once their time is over they become FINISHED on their own.
    // A postponed, suspended or cancelled match was not played, so its time passing changes nothing.
    private static final Set<MatchStatus> AUTO_FINISHABLE = EnumSet.of(SCHEDULED, RESCHEDULED, IN_PROGRESS);

    /** True when the stored text is a status that turns into FINISHED by itself when the match time is over. */
    public static boolean canFinishAutomatically(String status) {
        return Arrays.stream(values())
                .filter(known -> known.name().equals(status))
                .anyMatch(AUTO_FINISHABLE::contains);
    }

    /** The stored texts of those statuses, to query them. */
    public static List<String> autoFinishableNames() {
        return AUTO_FINISHABLE.stream().map(Enum::name).toList();
    }

    /** True when the stored text is a status of a match that has not been played yet; unknown text is not pending. */
    public static boolean isPending(String status) {
        return Arrays.stream(values())
                .filter(known -> known.name().equals(status))
                .anyMatch(PENDING::contains);
    }
}
