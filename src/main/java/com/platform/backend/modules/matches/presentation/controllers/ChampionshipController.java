package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IChampionshipService;
import com.platform.backend.modules.matches.presentation.requests.SetAdvancingTeamRequest.SetAdvancingTeamRequest;
import com.platform.backend.modules.matches.presentation.responses.ChampionshipResponse.ChampionshipResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Championship", description = "Phases, tables and rounds of an event's championship, driven by its format")
@RestController
@RequestMapping("/events/{eventId}/championship")
@RequiredArgsConstructor
public class ChampionshipController {

    private final IChampionshipService championshipService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChampionshipResponse>> getChampionship(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(championshipService.getChampionship(eventId)));
    }

    @PostMapping("/groups/draw")
    public ResponseEntity<ApiResponse<ChampionshipResponse>> drawGroups(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(championshipService.drawGroups(eventId)));
    }

    @PostMapping("/next")
    public ResponseEntity<ApiResponse<ChampionshipResponse>> generateNext(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(championshipService.generateNext(eventId)));
    }

    @PatchMapping("/matches/{matchId}/advancing-team")
    public ResponseEntity<ApiResponse<ChampionshipResponse>> setAdvancingTeam(@PathVariable UUID eventId,
                                                                             @PathVariable UUID matchId,
                                                                             @Valid @RequestBody SetAdvancingTeamRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(championshipService.setAdvancingTeam(eventId, matchId, request.getTeamId())));
    }
}
