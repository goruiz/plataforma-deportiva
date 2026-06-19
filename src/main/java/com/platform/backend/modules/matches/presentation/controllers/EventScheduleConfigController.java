package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IEventScheduleConfigService;
import com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest.SaveScheduleConfigRequest;
import com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse.EventScheduleConfigResponse;
import com.platform.backend.modules.matches.presentation.responses.GenerateMatchesResponse.GenerateMatchesResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Schedule Config", description = "Automatic match schedule generation")
@RestController
@RequestMapping("/events/{eventId}/schedule-config")
@RequiredArgsConstructor
public class EventScheduleConfigController {

    private final IEventScheduleConfigService scheduleConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<EventScheduleConfigResponse>> getConfig(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleConfigService.getConfig(eventId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventScheduleConfigResponse>> saveConfig(
            @PathVariable UUID eventId,
            @Valid @RequestBody SaveScheduleConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleConfigService.saveConfig(eventId, request)));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateMatchesResponse>> generate(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleConfigService.generateMatches(eventId)));
    }
}
