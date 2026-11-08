package com.platform.backend.modules.matches.presentation.responses.FreeSlotResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** A stretch of the day in which no match takes the court. */
@Data
@AllArgsConstructor
public class FreeSlotResponse {

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
