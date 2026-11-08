package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse.CourtAvailabilityResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAvailabilityServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    @Mock private IMatchesService matchesService;

    private CourtAvailabilityService courtAvailabilityService;
    private final UUID courtId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        courtAvailabilityService = new CourtAvailabilityService(matchesService, "08:00", "22:00");
    }

    private static MatchResponse match(int hour, int minute, String status) {
        MatchResponse match = new MatchResponse();
        match.setId(UUID.randomUUID());
        match.setMatchDate(DAY.atTime(hour, minute));
        match.setEndsAt(DAY.atTime(hour, minute).plusMinutes(75));
        match.setHomeTeamName("Tigres");
        match.setAwayTeamName("Leones");
        match.setStatus(status);
        return match;
    }

    @Test
    void getAvailability_whenTheCourtHasNoMatchesThatDay_thenItIsFreeAllDayWithinTheConfiguredHours() {
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of());

        CourtAvailabilityResponse availability = courtAvailabilityService.getAvailability(courtId, DAY);

        assertThat(availability.getCourtId()).isEqualTo(courtId);
        assertThat(availability.getDate()).isEqualTo(DAY);
        assertThat(availability.getOpensAt()).isEqualTo(LocalTime.of(8, 0));
        assertThat(availability.getClosesAt()).isEqualTo(LocalTime.of(22, 0));
        assertThat(availability.getBusySlots()).isEmpty();
        assertThat(availability.getFreeSlots()).singleElement().satisfies(free -> {
            assertThat(free.getStartsAt()).isEqualTo(DAY.atTime(8, 0));
            assertThat(free.getEndsAt()).isEqualTo(DAY.atTime(22, 0));
        });
    }

    @Test
    void getAvailability_whenAMatchTakesTheCourt_thenItIsBusyFromKickOffToItsEndAndTheRestIsFree() {
        MatchResponse scheduled = match(10, 0, "SCHEDULED");
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of(scheduled));

        CourtAvailabilityResponse availability = courtAvailabilityService.getAvailability(courtId, DAY);

        assertThat(availability.getBusySlots()).singleElement().satisfies(busy -> {
            assertThat(busy.getMatchId()).isEqualTo(scheduled.getId());
            assertThat(busy.getStartsAt()).isEqualTo(DAY.atTime(10, 0));
            assertThat(busy.getEndsAt()).isEqualTo(DAY.atTime(11, 15));
            assertThat(busy.getHomeTeamName()).isEqualTo("Tigres");
            assertThat(busy.getAwayTeamName()).isEqualTo("Leones");
            assertThat(busy.getStatus()).isEqualTo("SCHEDULED");
        });
        assertThat(availability.getFreeSlots()).extracting(free -> free.getStartsAt().toLocalTime(), free -> free.getEndsAt().toLocalTime())
                .containsExactly(
                        tuple(LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        tuple(LocalTime.of(11, 15), LocalTime.of(22, 0)));
    }

    @Test
    void getAvailability_whenAMatchIsCancelledPostponedOrSuspended_thenItDoesNotTakeTheCourt() {
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of(
                match(9, 0, "CANCELLED"), match(11, 0, "POSTPONED"), match(14, 0, "SUSPENDED")));

        CourtAvailabilityResponse availability = courtAvailabilityService.getAvailability(courtId, DAY);

        assertThat(availability.getBusySlots()).isEmpty();
        assertThat(availability.getFreeSlots()).hasSize(1);
    }

    @Test
    void getAvailability_whenAMatchWasAlreadyPlayed_thenItStillTookTheCourtThatDay() {
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of(match(9, 0, "FINISHED")));

        assertThat(courtAvailabilityService.getAvailability(courtId, DAY).getBusySlots()).hasSize(1);
    }

    @Test
    void getAvailability_whenAMatchIsOutsideTheOpeningHours_thenItIsListedBusyButChangesNothingFree() {
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of(match(23, 0, "SCHEDULED")));

        CourtAvailabilityResponse availability = courtAvailabilityService.getAvailability(courtId, DAY);

        assertThat(availability.getBusySlots()).hasSize(1);
        assertThat(availability.getFreeSlots()).singleElement()
                .satisfies(free -> assertThat(free.getEndsAt()).isEqualTo(LocalDateTime.of(DAY, LocalTime.of(22, 0))));
    }

    @Test
    void getAvailability_whenTheCourtIsNotVisibleToTheCaller_thenTheNotFoundOfTheMatchesLookupReachesTheCaller() {
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenThrow(new EntityNotFoundException("Court not found"));

        assertThatThrownBy(() -> courtAvailabilityService.getAvailability(courtId, DAY))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void constructor_whenTheCourtClosesBeforeItOpens_thenTheConfigurationIsRefused() {
        assertThatThrownBy(() -> new CourtAvailabilityService(matchesService, "22:00", "08:00"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CourtAvailabilityService(matchesService, "10:00", "10:00"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_whenTheHoursAreConfiguredDifferently_thenTheyAreUsed() {
        CourtAvailabilityService shorterDay = new CourtAvailabilityService(matchesService, "09:30", "20:00");
        when(matchesService.getByCourtIdOnDate(courtId, DAY)).thenReturn(List.of());

        CourtAvailabilityResponse availability = shorterDay.getAvailability(courtId, DAY);

        assertThat(availability.getOpensAt()).isEqualTo(LocalTime.of(9, 30));
        assertThat(availability.getClosesAt()).isEqualTo(LocalTime.of(20, 0));
        assertThat(availability.getFreeSlots()).singleElement()
                .satisfies(free -> assertThat(free.getStartsAt()).isEqualTo(DAY.atTime(9, 30)));
    }
}
