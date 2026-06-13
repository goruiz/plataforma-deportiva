package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.MatchResponse.MatchResponse;

import java.util.List;
import java.util.UUID;

public interface IEventScheduleConfigService {

    EventScheduleConfigResponse saveConfig(UUID eventId, SaveScheduleConfigRequest request);

    EventScheduleConfigResponse getConfig(UUID eventId);

    List<MatchResponse> generateMatches(UUID eventId);
}
