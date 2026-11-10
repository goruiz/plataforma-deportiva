package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.MatchRulesResponse.MatchRulesResponse;

import java.util.UUID;

public interface IMatchRulesService {

    /**
     * The rules the matches of an event follow: those of the organization the event belongs to. Throws
     * {@code EntityNotFoundException} when the event does not exist or the caller cannot see it.
     */
    MatchRulesResponse getForEvent(UUID eventId);
}
