package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.IMatchAttendanceService;
import com.platform.backend.modules.matches.presentation.requests.UpdateAttendanceRequest.UpdateAttendanceRequest;
import com.platform.backend.modules.matches.presentation.responses.MatchAttendanceResponse.MatchAttendanceResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Match attendance", description = "Whether the players will be at a match")
@RestController
@RequestMapping("/matches/{matchId}/attendance")
@RequiredArgsConstructor
public class MatchAttendanceController {

    private final IMatchAttendanceService matchAttendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<MatchAttendanceResponse>> get(@PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponse.ok(matchAttendanceService.getByMatchId(matchId)));
    }

    /** The signed-in player answers for themselves; the answer replaces any earlier one. */
    @PutMapping
    public ResponseEntity<ApiResponse<MatchAttendanceResponse>> respond(
            @PathVariable UUID matchId, @Valid @RequestBody UpdateAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(matchAttendanceService.respond(matchId, request)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<MatchAttendanceResponse>> withdraw(@PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponse.ok(matchAttendanceService.withdraw(matchId)));
    }
}
