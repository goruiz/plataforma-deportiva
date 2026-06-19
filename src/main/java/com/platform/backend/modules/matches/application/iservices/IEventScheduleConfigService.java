package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;

import java.util.UUID;

public interface IEventScheduleConfigService {

    EventScheduleConfigResponse saveConfig(UUID eventId, SaveScheduleConfigRequest request);

    EventScheduleConfigResponse getConfig(UUID eventId);

    GenerateMatchesResponse generateMatches(UUID eventId);
}
