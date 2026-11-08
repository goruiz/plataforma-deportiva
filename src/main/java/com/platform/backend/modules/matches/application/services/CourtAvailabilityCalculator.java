package com.platform.backend.modules.matches.application.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which stretches of a day a court has free: the opening hours minus what the matches take. Plain Java with no Spring
 * or database, so every case is tested with numbers.
 */
final class CourtAvailabilityCalculator {

    /** A stretch of time on one day: the end is exclusive, so two that touch leave no gap between them. */
    record Slot(LocalDateTime startsAt, LocalDateTime endsAt) {

        boolean isEmpty() {
            return !endsAt.isAfter(startsAt);
        }
    }

    private CourtAvailabilityCalculator() {}

    /**
     * What the busy slots leave free between {@code opensAt} and {@code closesAt} on {@code date}, in order. A busy slot
     * is cut to the opening hours (one that crosses the closing time only takes the court until then, one wholly outside
     * takes nothing), busy slots may overlap, and a stretch of zero minutes is not a free slot.
     */
    static List<Slot> freeSlots(LocalDate date, LocalTime opensAt, LocalTime closesAt, List<Slot> busySlots) {
        Slot openingHours = new Slot(date.atTime(opensAt), date.atTime(closesAt));
        List<Slot> free = new ArrayList<>();
        LocalDateTime cursor = openingHours.startsAt();

        for (Slot busy : busySlotsWithin(openingHours, busySlots)) {
            addIfNotEmpty(free, new Slot(cursor, busy.startsAt()));
            cursor = latest(cursor, busy.endsAt());
        }
        addIfNotEmpty(free, new Slot(cursor, openingHours.endsAt()));
        return free;
    }

    /** The busy slots cut to the opening hours, those left with some time only, by start. */
    private static List<Slot> busySlotsWithin(Slot openingHours, List<Slot> busySlots) {
        return busySlots.stream()
                .map(busy -> new Slot(latest(busy.startsAt(), openingHours.startsAt()),
                        earliest(busy.endsAt(), openingHours.endsAt())))
                .filter(clipped -> !clipped.isEmpty())
                .sorted(Comparator.comparing(Slot::startsAt))
                .toList();
    }

    private static void addIfNotEmpty(List<Slot> slots, Slot slot) {
        if (!slot.isEmpty()) {
            slots.add(slot);
        }
    }

    private static LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDateTime earliest(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }
}
