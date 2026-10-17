package com.platform.backend.modules.holidays.presentation.requests.CreateHolidayRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHolidayRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /** The first day of the break. */
    @NotNull
    private LocalDate date;

    /** The last day of a break that lasts several days. A single day (the same as {@code date}) when omitted. */
    private LocalDate endDate;

    /** Repeats every year on the same month and day. A one-off day when omitted. */
    private boolean recurring;
}
