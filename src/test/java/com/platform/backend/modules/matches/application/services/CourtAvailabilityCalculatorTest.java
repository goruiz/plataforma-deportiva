package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.services.CourtAvailabilityCalculator.Slot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourtAvailabilityCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);
    private static final LocalTime OPENS = LocalTime.of(8, 0);
    private static final LocalTime CLOSES = LocalTime.of(22, 0);

    private static LocalDateTime at(int hour, int minute) {
        return DAY.atTime(hour, minute);
    }

    private static Slot slot(int fromHour, int fromMinute, int toHour, int toMinute) {
        return new Slot(at(fromHour, fromMinute), at(toHour, toMinute));
    }

    private static List<Slot> freeWith(Slot... busy) {
        return CourtAvailabilityCalculator.freeSlots(DAY, OPENS, CLOSES, List.of(busy));
    }

    @Test
    void freeSlots_whenNoMatchTakesTheCourt_thenTheWholeOpeningHoursAreFree() {
        assertThat(freeWith()).containsExactly(slot(8, 0, 22, 0));
    }

    @Test
    void freeSlots_whenOneMatchIsInTheMiddle_thenTheTimeBeforeAndAfterIsFree() {
        assertThat(freeWith(slot(10, 0, 11, 15))).containsExactly(slot(8, 0, 10, 0), slot(11, 15, 22, 0));
    }

    @Test
    void freeSlots_whenSeveralMatchesFollowEachOther_thenOnlyTheGapsBetweenThemAreFree() {
        List<Slot> free = freeWith(slot(12, 0, 13, 15), slot(10, 0, 11, 15), slot(16, 0, 17, 15));

        assertThat(free).containsExactly(
                slot(8, 0, 10, 0), slot(11, 15, 12, 0), slot(13, 15, 16, 0), slot(17, 15, 22, 0));
    }

    @Test
    void freeSlots_whenMatchesTouch_thenThereIsNoGapOfZeroMinutesBetweenThem() {
        assertThat(freeWith(slot(10, 0, 11, 15), slot(11, 15, 12, 30)))
                .containsExactly(slot(8, 0, 10, 0), slot(12, 30, 22, 0));
    }

    @Test
    void freeSlots_whenMatchesOverlap_thenTheyAreOneBusyStretch() {
        assertThat(freeWith(slot(10, 0, 12, 0), slot(11, 0, 13, 0), slot(10, 30, 11, 0)))
                .containsExactly(slot(8, 0, 10, 0), slot(13, 0, 22, 0));
    }

    @Test
    void freeSlots_whenOneMatchContainsAnother_thenTheBiggerOneDecides() {
        assertThat(freeWith(slot(10, 0, 14, 0), slot(11, 0, 12, 0)))
                .containsExactly(slot(8, 0, 10, 0), slot(14, 0, 22, 0));
    }

    @Test
    void freeSlots_whenAMatchIsWhollyOutsideTheOpeningHours_thenItTakesNothing() {
        assertThat(freeWith(slot(6, 0, 7, 15), slot(22, 30, 23, 45))).containsExactly(slot(8, 0, 22, 0));
    }

    @Test
    void freeSlots_whenAMatchCrossesTheClosingTime_thenTheCourtIsFreeOnlyUntilItStarts() {
        assertThat(freeWith(slot(21, 30, 22, 45))).containsExactly(slot(8, 0, 21, 30));
    }

    @Test
    void freeSlots_whenAMatchStartsBeforeOpeningAndEndsAfter_thenItIsCutToTheOpeningHours() {
        assertThat(freeWith(slot(7, 0, 9, 0))).containsExactly(slot(9, 0, 22, 0));
    }

    @Test
    void freeSlots_whenAMatchEndsExactlyAtOpening_thenItTakesNothing() {
        assertThat(freeWith(slot(6, 45, 8, 0))).containsExactly(slot(8, 0, 22, 0));
    }

    @Test
    void freeSlots_whenAMatchCoversTheWholeOpeningHours_thenNothingIsFree() {
        assertThat(freeWith(slot(7, 0, 23, 0))).isEmpty();
    }

    @Test
    void freeSlots_whenTheFirstMatchStartsAtOpeningAndTheLastEndsAtClosing_thenNoEdgeSlotsAppear() {
        assertThat(freeWith(slot(8, 0, 9, 15), slot(20, 45, 22, 0))).containsExactly(slot(9, 15, 20, 45));
    }
}
