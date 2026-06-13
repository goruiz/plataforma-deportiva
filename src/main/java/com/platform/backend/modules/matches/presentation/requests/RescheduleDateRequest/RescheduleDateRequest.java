package com.platform.backend.modules.matches.presentation.requests.RescheduleDateRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RescheduleDateRequest {

    @NotNull
    private LocalDate fromDate;

    /** Null means postpone (sets status to POSTPONED without changing the date). */
    private LocalDate toDate;
}
