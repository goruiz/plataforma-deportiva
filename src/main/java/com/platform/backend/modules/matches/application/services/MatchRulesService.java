package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IMatchRulesService;
import com.platform.backend.modules.matches.presentation.responses.MatchRulesResponse.MatchRulesResponse;
import com.platform.backend.modules.settings.domain.models.SettingsValues;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchRulesService implements IMatchRulesService {

    private final MatchSettingsReader settingsReader;

    @Override
    public MatchRulesResponse getForEvent(UUID eventId) {
        SettingsValues rules = settingsReader.ofEvent(eventId);
        MatchRulesResponse response = new MatchRulesResponse();
        response.setDrawsAllowed(rules.drawsAllowed());
        response.setPenaltiesEnabled(rules.penaltiesEnabled());
        response.setExtraTimeMinutes(rules.extraTimeMinutes());
        response.setDisputeWindowHours(rules.disputeWindowHours());
        return response;
    }
}
