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

    // Still to be played at its scheduled time: not yet played, and not put off or called off.
    private static final Set<MatchStatus> AWAITING_KICK_OFF = EnumSet.of(SCHEDULED, RESCHEDULED);

    // A match in one of these does not take the court at its hour: it was called off or has no date that holds.
    private static final Set<MatchStatus> FREEING_THE_COURT = EnumSet.of(CANCELLED, POSTPONED, SUSPENDED);

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

    /** True when the match is still due to be played at its date (scheduled or rescheduled); unknown text is not. */
    public static boolean isAwaitingKickOff(String status) {
        return Arrays.stream(values())
                .filter(known -> known.name().equals(status))
                .anyMatch(AWAITING_KICK_OFF::contains);
    }

    /**
     * True when the match takes its court at its hour. Unknown text counts as taking it: better to say a court is busy
     * than to promise a free slot because of a status nobody recognises.
     */
    public static boolean occupiesCourt(String status) {
        return Arrays.stream(values())
                .filter(known -> known.name().equals(status))
                .noneMatch(FREEING_THE_COURT::contains);
    }

    /** True when the stored text is a status of a match that has not been played yet; unknown text is not pending. */
    public static boolean isPending(String status) {
        return Arrays.stream(values())
                .filter(known -> known.name().equals(status))
                .anyMatch(PENDING::contains);
    }
}
