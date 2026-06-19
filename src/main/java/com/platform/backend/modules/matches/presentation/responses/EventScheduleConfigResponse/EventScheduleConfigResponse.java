package com.platform.backend.modules.matches.presentation.responses.EventScheduleConfigResponse;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EventScheduleConfigResponse {

    private UUID id;
    private UUID eventId;
    private List<String> playDays;
    private String startTime;
    private int matchDurationMinutes;
    private int breakBetweenHalvesMinutes;
    private int breakBetweenMatchesMinutes;
    private UUID courtId;
    private List<LocalDate> blockedDates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
