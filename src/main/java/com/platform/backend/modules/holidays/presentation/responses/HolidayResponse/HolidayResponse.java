package com.platform.backend.modules.holidays.presentation.responses.HolidayResponse;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HolidayResponse {
    private UUID id;
    private String name;
    private LocalDate date;
    /** Always present: the same as {@code date} when the holiday lasts a single day. */
    private LocalDate endDate;
    private boolean recurring;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
