package com.platform.backend.modules.matches.application.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Finds where and when each match of an event goes. A match takes the first free slot of the first day on which it can
 * be played: a play day that is not blocked, on which neither of its teams plays already. Within that day the slots are
 * tried in order and, at each one, the match goes to the free court that has hosted the fewest matches of the event so
 * far, so the courts are used evenly and several matches can be played at the same time, each on its own court.
 * <p>
 * A court is free in a slot when no match holds it at any moment of it. What is already in the calendar is told to the
 * planner first (the event's matches that stay, and the matches of other events in the same courts); every slot given
 * is then booked, so the next match does not take it.
 * <p>
 * An event without courts to assign is planned with a single "court" that is {@code null}: its matches follow each
 * other in the slots of the day, as if they were played in one place, and keep no court.
 */
public final class CalendarPlanner {

    /** Where and when a match goes. The court is null when the event has no court to assign. */
    public record Slot(LocalDateTime kickOff, UUID courtId) {}

    private record Stretch(LocalDateTime from, LocalDateTime to) {
        boolean overlaps(LocalDateTime otherFrom, LocalDateTime otherTo) {
            return from.isBefore(otherTo) && otherFrom.isBefore(to);
        }
    }

    private final ScheduleRules rules;
    private final List<UUID> courtIds;
    private final Map<LocalDate, Set<UUID>> teamsByDay = new HashMap<>();
    // Court ids may be null (see the class comment), so these maps rely on HashMap accepting a null key.
    private final Map<UUID, List<Stretch>> stretchesByCourt = new HashMap<>();
    private final Map<UUID, Integer> matchCountByCourt = new HashMap<>();

    /**
     * @param courtIds the courts the matches are spread over, in the order ties are broken; a list with a single null
     *                 element when there is no court to assign (see {@link #withoutCourts})
     */
    public CalendarPlanner(ScheduleRules rules, List<UUID> courtIds) {
        if (courtIds.isEmpty()) {
            throw new IllegalArgumentException("A calendar needs at least one court (or the null court)");
        }
        this.rules = rules;
        // Not List.copyOf: the "no court" list holds a null, which it rejects.
        this.courtIds = Collections.unmodifiableList(new ArrayList<>(courtIds));
    }

    /** A planner for an event with no court to assign. */
    public static CalendarPlanner withoutCourts(ScheduleRules rules) {
        return new CalendarPlanner(rules, Collections.singletonList(null));
    }

    /**
     * A match that is already in the calendar and stays: its teams are busy that day and, when it holds its court, the
     * court is busy for the slot of the match. It also counts for the court's share of the matches.
     */
    public void addExistingMatch(UUID homeId, UUID awayId, LocalDateTime kickOff, UUID courtId, boolean holdsItsCourt) {
        markTeamsBusy(kickOff.toLocalDate(), homeId, awayId);
        if (holdsItsCourt) {
            blockCourt(courtId, kickOff, kickOff.plusMinutes(rules.slotDurationMinutes()));
            matchCountByCourt.merge(courtId, 1, Integer::sum);
        }
    }

    /** A court that is held by something else (a match of another event) from one moment to another. */
    public void blockCourt(UUID courtId, LocalDateTime from, LocalDateTime to) {
        stretchesByCourt.computeIfAbsent(courtId, key -> new ArrayList<>()).add(new Stretch(from, to));
    }

    /** The first slot, in time, in which both teams are free and a court is: empty when none is left before the limit. */
    public Optional<Slot> findFreeSlot(UUID homeId, UUID awayId) {
        for (LocalDate day = rules.startDate(); !day.isAfter(rules.searchLimit()); day = day.plusDays(1)) {
            if (!rules.isPlayable(day) || !areBothFree(day, homeId, awayId)) {
                continue;
            }
            Optional<Slot> slot = findFreeSlotOn(day);
            if (slot.isPresent()) {
                return slot;
            }
        }
        return Optional.empty();
    }

    /** Takes the slot for a match: its teams and its court are no longer free then. */
    public void book(Slot slot, UUID homeId, UUID awayId) {
        markTeamsBusy(slot.kickOff().toLocalDate(), homeId, awayId);
        blockCourt(slot.courtId(), slot.kickOff(), slot.kickOff().plusMinutes(rules.slotDurationMinutes()));
        matchCountByCourt.merge(slot.courtId(), 1, Integer::sum);
    }

    private Optional<Slot> findFreeSlotOn(LocalDate day) {
        for (int slotIndex = 0; ; slotIndex++) {
            Optional<LocalTime> time = rules.kickOffTime(slotIndex);
            if (time.isEmpty()) {
                return Optional.empty();
            }
            LocalDateTime kickOff = LocalDateTime.of(day, time.get());
            OptionalInt courtIndex = findLeastUsedFreeCourt(kickOff);
            if (courtIndex.isPresent()) {
                return Optional.of(new Slot(kickOff, courtIds.get(courtIndex.getAsInt())));
            }
        }
    }

    /** Among the courts free in the slot, the one with the fewest matches; the first of them when they are tied. */
    private OptionalInt findLeastUsedFreeCourt(LocalDateTime kickOff) {
        LocalDateTime slotEnd = kickOff.plusMinutes(rules.slotDurationMinutes());
        int bestIndex = -1;
        int bestCount = Integer.MAX_VALUE;

        for (int index = 0; index < courtIds.size(); index++) {
            UUID courtId = courtIds.get(index);
            int count = matchCountByCourt.getOrDefault(courtId, 0);
            if (count < bestCount && isCourtFree(courtId, kickOff, slotEnd)) {
                bestIndex = index;
                bestCount = count;
            }
        }
        return bestIndex < 0 ? OptionalInt.empty() : OptionalInt.of(bestIndex);
    }

    private boolean isCourtFree(UUID courtId, LocalDateTime from, LocalDateTime to) {
        return stretchesByCourt.getOrDefault(courtId, List.of()).stream().noneMatch(stretch -> stretch.overlaps(from, to));
    }

    private boolean areBothFree(LocalDate day, UUID homeId, UUID awayId) {
        Set<UUID> busyTeams = teamsByDay.getOrDefault(day, Collections.emptySet());
        return !busyTeams.contains(homeId) && !busyTeams.contains(awayId);
    }

    private void markTeamsBusy(LocalDate day, UUID homeId, UUID awayId) {
        Set<UUID> busyTeams = teamsByDay.computeIfAbsent(day, key -> new HashSet<>());
        busyTeams.add(homeId);
        busyTeams.add(awayId);
    }
}
