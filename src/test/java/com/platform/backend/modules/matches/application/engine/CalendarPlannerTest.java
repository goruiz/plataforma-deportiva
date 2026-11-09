package com.platform.backend.modules.matches.application.engine;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarPlannerTest {

    private static final LocalDate FIRST_SATURDAY = LocalDate.of(2099, 3, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    private static final UUID COURT_A = new UUID(1, 1);
    private static final UUID COURT_B = new UUID(1, 2);

    // Saturdays, 09:00, 60-minute matches and 15 minutes between them: slots at 09:00, 10:15, 11:30...
    private static ScheduleRules rules(Set<LocalDate> blockedDates) {
        return new ScheduleRules(Set.of(DayOfWeek.SATURDAY), blockedDates, LocalTime.of(9, 0), 60, 75,
                FIRST_SATURDAY, FIRST_SATURDAY.plusYears(1));
    }

    private static ScheduleRules rules() {
        return rules(Set.of());
    }

    private static UUID team(int number) {
        return new UUID(0, number);
    }

    private static LocalDateTime saturday(int weeksLater, int hour, int minute) {
        return FIRST_SATURDAY.plusWeeks(weeksLater).atTime(hour, minute);
    }

    private static CalendarPlanner.Slot book(CalendarPlanner planner, int home, int away) {
        CalendarPlanner.Slot slot = planner.findFreeSlot(team(home), team(away)).orElseThrow();
        planner.book(slot, team(home), team(away));
        return slot;
    }

    // ─── no court to assign ─────────────────────────────────────────────────────

    @Test
    void withoutCourts_thenTheMatchesOfADayFollowEachOtherInTheSlotsAndKeepNoCourt() {
        CalendarPlanner planner = CalendarPlanner.withoutCourts(rules());

        CalendarPlanner.Slot first = book(planner, 1, 2);
        CalendarPlanner.Slot second = book(planner, 3, 4);
        CalendarPlanner.Slot third = book(planner, 5, 6);

        assertThat(List.of(first.kickOff(), second.kickOff(), third.kickOff()))
                .containsExactly(saturday(0, 9, 0), saturday(0, 10, 15), saturday(0, 11, 30));
        assertThat(Arrays.asList(first.courtId(), second.courtId(), third.courtId())).containsOnlyNulls();
    }

    // ─── several courts ─────────────────────────────────────────────────────────

    @Test
    void withTwoCourts_thenTwoMatchesArePlayedAtTheSameTimeEachOnItsOwnCourt() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));

        CalendarPlanner.Slot first = book(planner, 1, 2);
        CalendarPlanner.Slot second = book(planner, 3, 4);
        CalendarPlanner.Slot third = book(planner, 5, 6);

        assertThat(first.kickOff()).isEqualTo(saturday(0, 9, 0));
        assertThat(second.kickOff()).isEqualTo(saturday(0, 9, 0));
        assertThat(List.of(first.courtId(), second.courtId())).containsExactlyInAnyOrder(COURT_A, COURT_B);
        assertThat(third.kickOff()).as("a third match has to wait for the next slot").isEqualTo(saturday(0, 10, 15));
    }

    @Test
    void withTwoCourts_thenTheCourtsTakeTurnsWhenOnlyOneMatchIsPlayedAtATime() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));

        // The same two teams play on successive Saturdays: one match a day, so only the balance decides the court.
        CalendarPlanner.Slot first = book(planner, 1, 2);
        CalendarPlanner.Slot second = book(planner, 1, 2);
        CalendarPlanner.Slot third = book(planner, 1, 2);
        CalendarPlanner.Slot fourth = book(planner, 1, 2);

        assertThat(List.of(first.courtId(), second.courtId(), third.courtId(), fourth.courtId()))
                .containsExactly(COURT_A, COURT_B, COURT_A, COURT_B);
        assertThat(List.of(first.kickOff(), second.kickOff(), third.kickOff(), fourth.kickOff()))
                .containsExactly(saturday(0, 9, 0), saturday(1, 9, 0), saturday(2, 9, 0), saturday(3, 9, 0));
    }

    @Test
    void existingMatchesCountForTheShareOfTheirCourts() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));
        planner.addExistingMatch(team(7), team(8), saturday(-3, 9, 0), COURT_A, true);
        planner.addExistingMatch(team(7), team(8), saturday(-2, 9, 0), COURT_A, true);

        assertThat(book(planner, 1, 2).courtId()).as("Cancha B has hosted fewer matches").isEqualTo(COURT_B);
    }

    // ─── what is already in the calendar ────────────────────────────────────────

    @Test
    void aCourtHeldByAnotherMatchIsNotFreeInTheSlotsItOverlaps() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));
        planner.blockCourt(COURT_A, saturday(0, 9, 30), saturday(0, 10, 30)); // overlaps the 09:00 and the 10:15 slots

        CalendarPlanner.Slot first = book(planner, 1, 2);
        CalendarPlanner.Slot second = book(planner, 3, 4);

        assertThat(first).isEqualTo(new CalendarPlanner.Slot(saturday(0, 9, 0), COURT_B));
        assertThat(second).as("Cancha A is busy until 10:30, so the next slot that fits it is 11:30, but B is free at 10:15")
                .isEqualTo(new CalendarPlanner.Slot(saturday(0, 10, 15), COURT_B));
    }

    @Test
    void aMatchThatDoesNotHoldItsCourtDoesNotBlockIt() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A));
        planner.addExistingMatch(team(7), team(8), saturday(0, 9, 0), COURT_A, false); // called off or postponed

        assertThat(book(planner, 1, 2).kickOff()).isEqualTo(saturday(0, 9, 0));
    }

    @Test
    void theTeamsOfAnExistingMatchDoNotPlayAgainThatDayEvenIfItHoldsNoCourt() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));
        planner.addExistingMatch(team(1), team(2), saturday(0, 9, 0), COURT_A, false);

        assertThat(book(planner, 1, 3).kickOff().toLocalDate()).isEqualTo(FIRST_SATURDAY.plusWeeks(1));
    }

    // ─── days ───────────────────────────────────────────────────────────────────

    @Test
    void aTeamPlaysAtMostOnceADayEvenWithCourtsToSpare() {
        CalendarPlanner planner = new CalendarPlanner(rules(), List.of(COURT_A, COURT_B));

        CalendarPlanner.Slot first = book(planner, 1, 2);
        CalendarPlanner.Slot second = book(planner, 1, 3);

        assertThat(first.kickOff().toLocalDate()).isEqualTo(FIRST_SATURDAY);
        assertThat(second.kickOff().toLocalDate()).isEqualTo(FIRST_SATURDAY.plusWeeks(1));
    }

    @Test
    void aBlockedDayIsSkipped() {
        CalendarPlanner planner = new CalendarPlanner(rules(Set.of(FIRST_SATURDAY)), List.of(COURT_A));

        assertThat(book(planner, 1, 2).kickOff().toLocalDate()).isEqualTo(FIRST_SATURDAY.plusWeeks(1));
    }

    @Test
    void aDayWithNoSlotLeftBeforeMidnightPassesTheMatchToTheNextPlayDay() {
        // 20:00 and 22:00 fit (120-minute matches); 00:00 would end after midnight.
        ScheduleRules lateRules = new ScheduleRules(Set.of(DayOfWeek.SATURDAY), Set.of(), LocalTime.of(20, 0), 120, 120,
                FIRST_SATURDAY, FIRST_SATURDAY.plusYears(1));
        CalendarPlanner planner = new CalendarPlanner(lateRules, List.of(COURT_A));

        book(planner, 1, 2);
        book(planner, 3, 4);
        CalendarPlanner.Slot third = book(planner, 5, 6);

        assertThat(third.kickOff()).isEqualTo(saturday(1, 20, 0));
    }

    @Test
    void whenNoDayIsPlayableBeforeTheLimitThereIsNoSlot() {
        ScheduleRules noPlayableDay = new ScheduleRules(Set.of(DayOfWeek.MONDAY), Set.of(), LocalTime.of(9, 0), 60, 75,
                FIRST_SATURDAY, FIRST_SATURDAY); // the search ends on a Saturday: there is no Monday in it

        assertThat(new CalendarPlanner(noPlayableDay, List.of(COURT_A)).findFreeSlot(team(1), team(2))).isEmpty();
    }

    @Test
    void aPlannerNeedsAtLeastOneCourt() {
        assertThatThrownBy(() -> new CalendarPlanner(rules(), List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
