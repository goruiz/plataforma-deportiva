package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;

import java.util.List;
import java.util.UUID;

public interface IPlayerStatsService {

    List<PlayerStatsResponse> getPlayerStatsByEventId(UUID eventId);
}
