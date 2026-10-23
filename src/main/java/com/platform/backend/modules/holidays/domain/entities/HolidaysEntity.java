package com.platform.backend.modules.holidays.domain.entities;

import org.hibernate.annotations.Filter;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import com.platform.backend.shared.domain.tenancy.OrganizationOwned;
import com.platform.backend.shared.infraestructure.tenancy.OrganizationOwnerListener;
import jakarta.persistence.EntityListeners;
import java.util.UUID;
import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A day, or a run of consecutive days, on which no match is scheduled: a public holiday or any break
 * the administrator chooses not to play on.
 */
@Entity
@Table(name = "holidays")
@EntityListeners(OrganizationOwnerListener.class)
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OWN_ORGANIZATION, deduceAliasInjectionPoints = false)
public class HolidaysEntity extends BaseEntity implements OrganizationOwned {

    /**
     * The longest a break can last after its first day. With this cap a recurring break reaches at most into
     * the following year, which is what lets {@link #occurrencesBetween} look one year back and no further.
     */
    public static final long MAX_DAYS_AFTER_START = 365;

    // Recurring holidays shift against the calendar (a February 29th), so two of them are compared over a leap cycle.
    private static final int YEARS_COMPARED_WHEN_RECURRING = 4;

    @Column(length = 100, nullable = false)
    private String name;

    /** The first day. For a recurring holiday only its month and day matter. */
    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    /** The last day of a break that lasts several days. Null (every row saved before ranges existed) means a single day. */
    @Column(name = "holiday_end_date")
    private LocalDate endDate;

    /** True when the holiday repeats on the same month and day every year (Christmas); false for a one-off day. */
    @Column(nullable = false)
    private boolean recurring;

    /** The customer this record belongs to; null when it belongs to the platform owner. */
    @Column(name = "organization_id")
    private UUID organizationId;

    public HolidaysEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    /** The last day of the break; the same as {@link #getDate()} when it lasts a single day. */
    public LocalDate getEndDate() { return endDate != null ? endDate : date; }

    /** How many days the break lasts after its first one: 0 for a single day, negative if the end is before the start. */
    public long daysAfterStart() {
        return ChronoUnit.DAYS.between(date, getEndDate());
    }

    public boolean appliesOn(LocalDate day) {
        return !occurrencesBetween(day, day).isEmpty();
    }

    /** The days within {@code [from, to]} (both included) on which this holiday falls, in order and without repeats. */
    public List<LocalDate> occurrencesBetween(LocalDate from, LocalDate to) {
        long daysAfterStart = daysAfterStart();
        return startsOfOccurrencesReaching(from, to)
                .flatMap(start -> start.datesUntil(start.plusDays(daysAfterStart + 1)))
                .filter(day -> !day.isBefore(from) && !day.isAfter(to))
                .distinct()
                .sorted()
                .toList();
    }

    /** True when both holidays share at least one day, so registering both would say the same thing twice. */
    public boolean clashesWith(HolidaysEntity other) {
        LocalDate from = date.isBefore(other.date) ? date : other.date;
        LocalDate lastDay = getEndDate().isAfter(other.getEndDate()) ? getEndDate() : other.getEndDate();
        LocalDate to = recurring || other.recurring ? lastDay.plusYears(YEARS_COMPARED_WHEN_RECURRING) : lastDay;

        Set<LocalDate> myDays = new HashSet<>(occurrencesBetween(from, to));
        return other.occurrencesBetween(from, to).stream().anyMatch(myDays::contains);
    }

    // A break that starts late in one year can run into the next, so for a recurring holiday the year before `from` counts too.
    private Stream<LocalDate> startsOfOccurrencesReaching(LocalDate from, LocalDate to) {
        if (!recurring) {
            return Stream.of(date);
        }
        return IntStream.rangeClosed(from.getYear() - 1, to.getYear()).mapToObj(this::occurrenceIn);
    }

    // A February 29th holiday lands on February 28th in the years that have no 29th (java.time's rule).
    private LocalDate occurrenceIn(int year) {
        return MonthDay.from(date).atYear(year);
    }

    @Override
    public UUID getOrganizationId() { return organizationId; }

    @Override
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
