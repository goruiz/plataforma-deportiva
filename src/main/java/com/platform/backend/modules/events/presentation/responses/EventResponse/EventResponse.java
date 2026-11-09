package com.platform.backend.modules.events.presentation.responses.EventResponse;

import com.platform.backend.modules.events.presentation.responses.EventFormatResponse.EventFormatResponse;
import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EventResponse {

    private UUID id;
    private String name;
    private String description;
    /** Name of {@link #eventFormat}; kept so clients that only read the format as text (the mobile app) keep working. */
    private String format;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer minPlayersPerTeam;
    private Integer maxPlayersPerTeam;
    private EventTypeResponse eventType;
    private EventFormatResponse eventFormat;
    /** The customer organization the event belongs to; null for the platform owner's own events. */
    private UUID organizationId;

    /** Whether every player in the app sees the event, whatever their organization. */
    private Boolean isPublic;

    /**
     * The complexes the event is played in. Only the event endpoints fill it (null elsewhere, e.g. the events of a type
     * the mobile app lists), and an event of another organization that a player sees because it is public shows none.
     */
    private List<UUID> sportComplexIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
