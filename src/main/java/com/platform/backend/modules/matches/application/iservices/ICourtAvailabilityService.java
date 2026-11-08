package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse.CourtAvailabilityResponse;

import java.time.LocalDate;
import java.util.UUID;

/** What a court has free on a day, worked out from the matches scheduled in it. */
public interface ICourtAvailabilityService {

    /**
     * The matches that take the court that day and the stretches inside the opening hours they leave free. A court that
     * does not exist or that the caller cannot see is a 404.
     */
    CourtAvailabilityResponse getAvailability(UUID courtId, LocalDate date);
}
