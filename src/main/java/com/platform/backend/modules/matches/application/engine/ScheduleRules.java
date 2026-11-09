package com.platform.backend.modules.matches.application.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

/**
 * Which days a match may be played on, and at what time each slot of a day starts. A slot is the duration of a match
 * plus the break before the next one; the first slot of a day starts at {@code firstKickOff}.
 */
public record ScheduleRules(Set<DayOfWeek> playDays, Set<LocalDate> blockedDates, LocalTime firstKickOff,
                            int matchDurationMinutes, int slotDurationMinutes,
                            LocalDate startDate, LocalDate searchLimit) {

    private static final int MINUTES_PER_DAY = 24 * 60;

    public boolean isPlayable(LocalDate day) {
        return playDays.contains(day.getDayOfWeek()) && !blockedDates.contains(day);
    }

    /** Kick-off of the slot with that index (0 is the first of the day); empty when the match would end after midnight. */
    public Optional<LocalTime> kickOffTime(int slotIndex) {
        long startMinute = firstKickOff.toSecondOfDay() / 60 + (long) slotIndex * slotDurationMinutes;
        if (startMinute + matchDurationMinutes > MINUTES_PER_DAY) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of((int) (startMinute / 60), (int) (startMinute % 60)));
    }
}
