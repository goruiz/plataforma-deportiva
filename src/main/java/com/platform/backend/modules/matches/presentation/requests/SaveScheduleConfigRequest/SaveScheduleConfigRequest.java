package com.platform.backend.modules.matches.presentation.requests.SaveScheduleConfigRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveScheduleConfigRequest {

    /** e.g. ["MONDAY", "WEDNESDAY", "FRIDAY"] */
    @NotEmpty
    private List<String> playDays;

    /** HH:mm format, e.g. "09:00" */
    @NotNull
    private String startTime;

    @Min(1)
    private int matchDurationMinutes;

    @Min(0)
    private int breakBetweenHalvesMinutes = 0;

    @Min(0)
    private int breakBetweenMatchesMinutes;

    private UUID courtId;
}
