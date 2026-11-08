package com.platform.backend.modules.matches.presentation.responses.BusySlotResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** A match that takes the court: from its kick-off until it ends, break included (as in the match's {@code endsAt}). */
@Data
@AllArgsConstructor
public class BusySlotResponse {

    private UUID matchId;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    /** Null when the caller does not see that team. */
    private String homeTeamName;
    private String awayTeamName;
    private String status;
}
