package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IStandingsService;
import com.platform.backend.modules.matches.presentation.responses.StandingEntryResponse.StandingEntryResponse;
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

@Tag(name = "Standings", description = "League standings table")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class StandingsController {

    private final IStandingsService standingsService;

    @GetMapping("/{eventId}/standings")
    public ResponseEntity<ApiResponse<List<StandingEntryResponse>>> getStandings(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(standingsService.getStandingsByEventId(eventId)));
    }
}
