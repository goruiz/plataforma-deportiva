package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IPlayerStatsService;
import com.platform.backend.modules.matches.presentation.responses.PlayerStatsResponse.PlayerStatsResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Player stats", description = "Scorers, assists and cards of an event")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final IPlayerStatsService playerStatsService;

    @GetMapping("/{eventId}/player-stats")
    public ResponseEntity<ApiResponse<List<PlayerStatsResponse>>> getPlayerStats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(playerStatsService.getPlayerStatsByEventId(eventId)));
    }
}
