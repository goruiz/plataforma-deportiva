package com.platform.backend.modules.matches.presentation.responses.MatchEventResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MatchEventResponse {

    private UUID id;
    private UUID matchId;
    private UUID playerId;
    /** The player's full name, so a timeline can be shown without reading every player. Null if the player was deleted. */
    private String playerName;
    private UUID teamId;
    private String eventType;
    private Integer minute;
    private String notes;
    /** Only for a substitution: the player who goes out ({@link #playerId} is the one who comes in). */
    private UUID replacedPlayerId;
    /** The name of {@link #replacedPlayerId}; null when there is none or the player was deleted. */
    private String replacedPlayerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
