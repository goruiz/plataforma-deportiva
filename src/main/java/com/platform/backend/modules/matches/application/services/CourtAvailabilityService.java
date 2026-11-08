package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.ICourtAvailabilityService;
import com.platform.backend.modules.matches.application.iservices.IMatchesService;
import com.platform.backend.modules.matches.application.services.CourtAvailabilityCalculator.Slot;
import com.platform.backend.modules.matches.domain.enums.MatchStatus;
import com.platform.backend.modules.matches.presentation.responses.BusySlotResponse.BusySlotResponse;
import com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse.CourtAvailabilityResponse;
import com.platform.backend.modules.matches.presentation.responses.FreeSlotResponse.FreeSlotResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * A court has no hours of its own, so what is free is the opening hours of the platform minus what the matches take.
 * A match takes the court from its kick-off to its {@code endsAt} (duration and break of its event, as everywhere
 * else); one that is cancelled, postponed or suspended does not.
 */
@Service
public class CourtAvailabilityService implements ICourtAvailabilityService {

    private final IMatchesService matchesService;
    private final LocalTime opensAt;
    private final LocalTime closesAt;

    public CourtAvailabilityService(IMatchesService matchesService,
                                    @Value("${app.courts.opening-time:08:00}") String openingTime,
                                    @Value("${app.courts.closing-time:22:00}") String closingTime) {
        this.matchesService = matchesService;
        this.opensAt = LocalTime.parse(openingTime);
        this.closesAt = LocalTime.parse(closingTime);
        if (!closesAt.isAfter(opensAt)) {
            throw new IllegalStateException("app.courts.closing-time must be after app.courts.opening-time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CourtAvailabilityResponse getAvailability(UUID courtId, LocalDate date) {
        List<MatchResponse> matchesThatTakeTheCourt = matchesService.getByCourtIdOnDate(courtId, date).stream()
                .filter(match -> MatchStatus.occupiesCourt(match.getStatus()))
                .toList();

        List<BusySlotResponse> busySlots = matchesThatTakeTheCourt.stream().map(CourtAvailabilityService::toBusySlot).toList();
        List<Slot> busyStretches = matchesThatTakeTheCourt.stream()
                .map(match -> new Slot(match.getMatchDate(), match.getEndsAt()))
                .toList();
        List<FreeSlotResponse> freeSlots = CourtAvailabilityCalculator.freeSlots(date, opensAt, closesAt, busyStretches).stream()
                .map(slot -> new FreeSlotResponse(slot.startsAt(), slot.endsAt()))
                .toList();
        return new CourtAvailabilityResponse(courtId, date, opensAt, closesAt, busySlots, freeSlots);
    }

    private static BusySlotResponse toBusySlot(MatchResponse match) {
        return new BusySlotResponse(match.getId(), match.getMatchDate(), match.getEndsAt(),
                match.getHomeTeamName(), match.getAwayTeamName(), match.getStatus());
    }
}
