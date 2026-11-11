package com.platform.backend.modules.holidays.presentation.controllers;

import com.platform.backend.modules.holidays.application.iservices.IHolidaysService;
import com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest.CreateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest.UpdateHolidayRequest;
import com.platform.backend.modules.holidays.presentation.responses.HolidayResponse.HolidayResponse;
import com.platform.backend.shared.presentation.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import com.platform.backend.shared.infraestructure.security.AccessRules;

import java.util.List;
import java.util.UUID;

@Tag(name = "Holidays", description = "Holidays and other days on which no matches are scheduled")
@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidaysController {

    private final IHolidaysService holidaysService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(holidaysService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(holidaysService.getById(id)));
    }

    // Only an administrator creates, changes or deletes these: the app only reads them (the organization filter
    // already keeps each administrator inside their own).
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> create(@Valid @RequestBody CreateHolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(holidaysService.create(request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateHolidayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(holidaysService.update(id, request)));
    }

    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        holidaysService.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
