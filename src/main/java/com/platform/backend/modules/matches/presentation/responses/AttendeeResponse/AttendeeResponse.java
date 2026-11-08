package com.platform.backend.modules.matches.presentation.responses.AttendeeResponse;

import com.platform.backend.modules.matches.domain.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/** One player's answer about a match. */
@Data
@AllArgsConstructor
public class AttendeeResponse {

    private UUID playerId;
    /** Null if the player was deleted after answering. */
    private String playerName;
    /** The team the player answered for. */
    private UUID teamId;
    private AttendanceStatus status;
}
