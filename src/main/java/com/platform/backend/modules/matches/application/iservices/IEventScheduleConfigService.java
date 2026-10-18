package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.application.engine.MatchDraft;
import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;

import java.util.List;
import java.util.UUID;

public interface IEventScheduleConfigService {

    EventScheduleConfigResponse saveConfig(UUID eventId, SaveScheduleConfigRequest request);

    EventScheduleConfigResponse getConfig(UUID eventId);

    GenerateMatchesResponse generateMatches(UUID eventId);

    /**
     * Creates the given championship matches on the first free slots of the event's calendar.
     *
     * @throws IllegalStateException {@code SCHEDULE_CONFIG_REQUIRED} when the calendar configuration was never saved
     */
    List<MatchResponse> scheduleDrafts(UUID eventId, List<MatchDraft> drafts);
}
