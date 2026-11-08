package com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse;

import com.platform.backend.modules.matches.presentation.responses.BusySlotResponse.BusySlotResponse;
import com.platform.backend.modules.matches.presentation.responses.FreeSlotResponse.FreeSlotResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * What a court has free on a day. It is worked out from the matches scheduled in it and from the opening hours of the
 * platform ({@code app.courts.*}): a court has no hours of its own.
 */
@Data
@AllArgsConstructor
public class CourtAvailabilityResponse {

    private UUID courtId;
    private LocalDate date;
    private LocalTime opensAt;
    private LocalTime closesAt;
    /** Every match of that day that takes the court, even one that falls outside the opening hours; by kick-off. */
    private List<BusySlotResponse> busySlots;
    /** What the busy slots leave inside the opening hours, in order and never empty. */
    private List<FreeSlotResponse> freeSlots;
}
