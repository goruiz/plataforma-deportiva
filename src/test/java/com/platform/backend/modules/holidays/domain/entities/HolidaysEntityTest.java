package com.platform.backend.modules.holidays.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HolidaysEntityTest {

    private static HolidaysEntity holiday(LocalDate date, boolean recurring) {
        HolidaysEntity holiday = new HolidaysEntity();
        holiday.setName("Holiday");
        holiday.setDate(date);
        holiday.setRecurring(recurring);
        return holiday;
    }

    private static HolidaysEntity holiday(LocalDate date, LocalDate endDate, boolean recurring) {
        HolidaysEntity holiday = holiday(date, recurring);
        holiday.setEndDate(endDate);
        return holiday;
    }

    // ─── duration ───────────────────────────────────────────────────────────────

    @Test
    void getEndDate_whenNoneWasSet_thenItIsASingleDay() {
        HolidaysEntity singleDay = holiday(LocalDate.of(2026, 7, 4), false);

        assertThat(singleDay.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(singleDay.daysAfterStart()).isZero();
    }

    @Test
    void daysAfterStart_whenTheBreakLastsThreeDays_thenTwo() {
        HolidaysEntity threeDays = holiday(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 6), false);

        assertThat(threeDays.daysAfterStart()).isEqualTo(2);
    }

    // ─── appliesOn ──────────────────────────────────────────────────────────────

    // ─── appliesOn ──────────────────────────────────────────────────────────────

    @Test
    void appliesOn_whenARange_thenEveryDayFromStartToEndIncluded() {
        HolidaysEntity carnival = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10), false);

        assertThat(carnival.appliesOn(LocalDate.of(2027, 2, 7))).isFalse();
        assertThat(carnival.appliesOn(LocalDate.of(2027, 2, 8))).isTrue();
        assertThat(carnival.appliesOn(LocalDate.of(2027, 2, 9))).isTrue();
        assertThat(carnival.appliesOn(LocalDate.of(2027, 2, 10))).isTrue();
        assertThat(carnival.appliesOn(LocalDate.of(2027, 2, 11))).isFalse();
    }

    @Test
    void appliesOn_whenARecurringRangeCrossesNewYear_thenItCoversBothSidesInEveryYear() {
        HolidaysEntity winterBreak = holiday(LocalDate.of(2020, 12, 24), LocalDate.of(2021, 1, 2), true);

        assertThat(winterBreak.appliesOn(LocalDate.of(2026, 12, 23))).isFalse();
        assertThat(winterBreak.appliesOn(LocalDate.of(2026, 12, 24))).isTrue();
        assertThat(winterBreak.appliesOn(LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(winterBreak.appliesOn(LocalDate.of(2027, 1, 1))).isTrue(); // from the previous year's occurrence
        assertThat(winterBreak.appliesOn(LocalDate.of(2027, 1, 2))).isTrue();
        assertThat(winterBreak.appliesOn(LocalDate.of(2027, 1, 3))).isFalse();
    }


    @Test
    void appliesOn_whenOneOff_thenOnlyItsOwnDay() {
        HolidaysEntity oneOff = holiday(LocalDate.of(2026, 7, 4), false);

        assertThat(oneOff.appliesOn(LocalDate.of(2026, 7, 4))).isTrue();
        assertThat(oneOff.appliesOn(LocalDate.of(2027, 7, 4))).isFalse();
        assertThat(oneOff.appliesOn(LocalDate.of(2026, 7, 5))).isFalse();
    }

    @Test
    void appliesOn_whenRecurring_thenSameMonthAndDayInAnyYear() {
        HolidaysEntity christmas = holiday(LocalDate.of(2020, 12, 25), true);

        assertThat(christmas.appliesOn(LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(christmas.appliesOn(LocalDate.of(2031, 12, 25))).isTrue();
        assertThat(christmas.appliesOn(LocalDate.of(2026, 12, 24))).isFalse();
    }

    @Test
    void appliesOn_whenRecurringOnLeapDay_thenFallsOnFebruary28InYearsWithoutIt() {
        HolidaysEntity leapDay = holiday(LocalDate.of(2024, 2, 29), true);

        assertThat(leapDay.appliesOn(LocalDate.of(2028, 2, 29))).isTrue();
        assertThat(leapDay.appliesOn(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(leapDay.appliesOn(LocalDate.of(2027, 3, 1))).isFalse();
    }

    // ─── occurrencesBetween ─────────────────────────────────────────────────────

    @Test
    void occurrencesBetween_whenOneOffInsideRange_thenItsDay() {
        HolidaysEntity oneOff = holiday(LocalDate.of(2026, 7, 4), false);

        assertThat(oneOff.occurrencesBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .containsExactly(LocalDate.of(2026, 7, 4));
    }

    @Test
    void occurrencesBetween_whenOneOffOutsideRange_thenNothing() {
        HolidaysEntity oneOff = holiday(LocalDate.of(2026, 7, 4), false);

        assertThat(oneOff.occurrencesBetween(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).isEmpty();
    }

    @Test
    void occurrencesBetween_whenRangeIncludesBothBounds_thenBoundsCount() {
        HolidaysEntity oneOff = holiday(LocalDate.of(2026, 7, 4), false);

        assertThat(oneOff.occurrencesBetween(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4)))
                .containsExactly(LocalDate.of(2026, 7, 4));
    }

    @Test
    void occurrencesBetween_whenRecurringAndRangeSpansYears_thenOneOccurrencePerYear() {
        HolidaysEntity christmas = holiday(LocalDate.of(2020, 12, 25), true);

        assertThat(christmas.occurrencesBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2028, 6, 1)))
                .containsExactly(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 12, 25));
    }

    @Test
    void occurrencesBetween_whenRecurringOccurrenceFallsOutsideTheRangeOfItsYear_thenSkipsIt() {
        HolidaysEntity newYear = holiday(LocalDate.of(2020, 1, 1), true);

        // The range starts after January 1st 2026 and ends before January 1st 2027.
        assertThat(newYear.occurrencesBetween(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31))).isEmpty();
    }

    @Test
    void occurrencesBetween_whenARange_thenEachOfItsDaysInOrder() {
        HolidaysEntity carnival = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 10), false);

        assertThat(carnival.occurrencesBetween(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28)))
                .containsExactly(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 9), LocalDate.of(2027, 2, 10));
    }

    @Test
    void occurrencesBetween_whenTheWindowCutsARange_thenOnlyTheDaysInsideIt() {
        HolidaysEntity carnival = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 12), false);

        assertThat(carnival.occurrencesBetween(LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 11)))
                .containsExactly(LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 11));
    }

    @Test
    void occurrencesBetween_whenARecurringRangeStartedInThePreviousYear_thenItsDaysInTheWindowCount() {
        HolidaysEntity winterBreak = holiday(LocalDate.of(2020, 12, 24), LocalDate.of(2021, 1, 2), true);

        // The window opens after the break began (December 2026) and only sees its January days.
        assertThat(winterBreak.occurrencesBetween(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)))
                .containsExactly(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2));
    }

    @Test
    void occurrencesBetween_whenARecurringRangeSpansYears_thenEveryDayOfEveryOccurrenceWithoutRepeats() {
        HolidaysEntity winterBreak = holiday(LocalDate.of(2020, 12, 30), LocalDate.of(2021, 1, 2), true);

        assertThat(winterBreak.occurrencesBetween(LocalDate.of(2026, 12, 1), LocalDate.of(2028, 1, 31)))
                .containsExactly(
                        LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2),
                        LocalDate.of(2027, 12, 30), LocalDate.of(2027, 12, 31), LocalDate.of(2028, 1, 1), LocalDate.of(2028, 1, 2));
    }

    // ─── clashesWith ────────────────────────────────────────────────────────────

    @Test
    void clashesWith_whenSameDay_thenTrue() {
        assertThat(holiday(LocalDate.of(2026, 7, 4), false).clashesWith(holiday(LocalDate.of(2026, 7, 4), false))).isTrue();
    }

    @Test
    void clashesWith_whenDifferentDays_thenFalse() {
        assertThat(holiday(LocalDate.of(2026, 7, 4), false).clashesWith(holiday(LocalDate.of(2026, 7, 5), false))).isFalse();
    }

    @Test
    void clashesWith_whenOneIsRecurringAndTheOtherFallsOnItsDay_thenTrueInBothDirections() {
        HolidaysEntity recurring = holiday(LocalDate.of(2020, 12, 25), true);
        HolidaysEntity oneOff = holiday(LocalDate.of(2026, 12, 25), false);

        assertThat(recurring.clashesWith(oneOff)).isTrue();
        assertThat(oneOff.clashesWith(recurring)).isTrue();
    }

    @Test
    void clashesWith_whenBothRecurringOnTheSameMonthAndDay_thenTrue() {
        assertThat(holiday(LocalDate.of(2020, 12, 25), true).clashesWith(holiday(LocalDate.of(2026, 12, 25), true))).isTrue();
    }

    @Test
    void clashesWith_whenTwoRangesOverlap_thenTrueInBothDirections() {
        HolidaysEntity first = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 12), false);
        HolidaysEntity second = holiday(LocalDate.of(2027, 2, 12), LocalDate.of(2027, 2, 15), false); // shares only the 12th

        assertThat(first.clashesWith(second)).isTrue();
        assertThat(second.clashesWith(first)).isTrue();
    }

    @Test
    void clashesWith_whenARangeContainsTheOtherHolidayInItsMiddle_thenTrue() {
        HolidaysEntity range = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 12), false);

        assertThat(range.clashesWith(holiday(LocalDate.of(2027, 2, 10), false))).isTrue();
        assertThat(holiday(LocalDate.of(2027, 2, 10), false).clashesWith(range)).isTrue();
    }

    @Test
    void clashesWith_whenTwoRangesAreBackToBackWithoutSharingADay_thenFalse() {
        HolidaysEntity first = holiday(LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 12), false);
        HolidaysEntity second = holiday(LocalDate.of(2027, 2, 13), LocalDate.of(2027, 2, 15), false);

        assertThat(first.clashesWith(second)).isFalse();
        assertThat(second.clashesWith(first)).isFalse();
    }

    @Test
    void clashesWith_whenARecurringRangeCoversAOneOffDayInAnotherYear_thenTrue() {
        HolidaysEntity winterBreak = holiday(LocalDate.of(2020, 12, 24), LocalDate.of(2021, 1, 2), true);
        HolidaysEntity newYearDay2029 = holiday(LocalDate.of(2029, 1, 1), false);

        assertThat(winterBreak.clashesWith(newYearDay2029)).isTrue();
        assertThat(newYearDay2029.clashesWith(winterBreak)).isTrue();
    }

    @Test
    void clashesWith_whenARecurringRangeDoesNotReachTheOtherDay_thenFalse() {
        HolidaysEntity winterBreak = holiday(LocalDate.of(2020, 12, 24), LocalDate.of(2021, 1, 2), true);

        assertThat(winterBreak.clashesWith(holiday(LocalDate.of(2029, 1, 3), false))).isFalse();
    }
}
