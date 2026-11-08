package com.platform.backend.modules.matches.application.iservices;

import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;
import com.platform.backend.modules.matches.presentation.responses.PlayerTotalsResponse.PlayerTotalsResponse;

import java.util.List;
import java.util.UUID;

public interface IPlayerStatsService {

    List<PlayerStatsResponse> getPlayerStatsByEventId(UUID eventId);

    /**
     * What a player has done across every event, the ones of a team they left included. A 404 for a player the caller
     * cannot see.
     */
    PlayerTotalsResponse getPlayerTotals(UUID playerId);

    /** The totals of the player that goes with the account that is calling; a 404 if the account has none. */
    PlayerTotalsResponse getCurrentPlayerTotals();
}
