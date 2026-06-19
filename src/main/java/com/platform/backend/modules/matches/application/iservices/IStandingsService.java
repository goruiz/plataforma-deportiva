package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;

import java.util.List;
import java.util.UUID;

public interface IStandingsService {

    List<StandingEntryResponse> getStandingsByEventId(UUID eventId);
}
