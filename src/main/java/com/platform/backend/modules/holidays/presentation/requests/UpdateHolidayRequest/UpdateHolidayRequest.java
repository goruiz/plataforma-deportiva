package com.platform.backend.modules.holidays.presentation.requests.UpdateHolidayRequest;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateHolidayRequest {

    // Null means "leave unchanged"; a name, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 100)
    private String name;

    private LocalDate date;

    // Null leaves the end as it is, unless `date` moves: then the break keeps its length. To turn a
    // multi-day break back into a single day, send the same value as `date`.
    private LocalDate endDate;

    private Boolean recurring;
}
