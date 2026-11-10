package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchRulesService;
import com.platform.backend.modules.matches.presentation.responses.MatchRulesResponse.MatchRulesResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Match rules", description = "The rules the matches of an event follow")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class MatchRulesController {

    private final IMatchRulesService matchRulesService;

    /**
     * The rules of the organization the event belongs to. They can differ from the caller's own (the platform owner
     * sees events of every organization), so a screen about one event reads them here and not from {@code /settings}.
     */
    @GetMapping("/{eventId}/match-rules")
    public ResponseEntity<ApiResponse<MatchRulesResponse>> getForEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(matchRulesService.getForEvent(eventId)));
    }
}
