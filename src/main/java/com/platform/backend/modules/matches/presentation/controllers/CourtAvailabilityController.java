package com.platform.backend.modules.matches.presentation.controllers;

import com.platform.backend.modules.matches.application.iservices.ICourtAvailabilityService;
import com.platform.backend.modules.matches.presentation.responses.CourtAvailabilityResponse.CourtAvailabilityResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

// Lives with the matches because what a court has free is decided by the matches scheduled in it.
@Tag(name = "Court availability", description = "Free and busy hours of a court")
@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtAvailabilityController {

    private final ICourtAvailabilityService courtAvailabilityService;

    @GetMapping("/{courtId}/availability")
    public ResponseEntity<ApiResponse<CourtAvailabilityResponse>> getAvailability(
            @PathVariable UUID courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(courtAvailabilityService.getAvailability(courtId, date)));
    }
}
