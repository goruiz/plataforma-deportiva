package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IPlayerStatsService;
import com.platform.backend.modules.matches.presentation.responses.PlayerTotalsResponse.PlayerTotalsResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Lives with the matches because the totals are counted from the events of the matches a player took part in.
@Tag(name = "Player totals", description = "What a player has done across every event")
@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerTotalsController {

    private final IPlayerStatsService playerStatsService;

    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<PlayerTotalsResponse>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.ok(playerStatsService.getCurrentPlayerTotals()));
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<ApiResponse<PlayerTotalsResponse>> getByPlayerId(@PathVariable UUID playerId) {
        return ResponseEntity.ok(ApiResponse.ok(playerStatsService.getPlayerTotals(playerId)));
    }
}
