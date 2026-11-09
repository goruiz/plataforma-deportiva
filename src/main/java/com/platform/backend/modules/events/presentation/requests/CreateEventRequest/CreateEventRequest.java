package com.platform.backend.modules.events.presentation.requests.CreateEventRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEventRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 20)
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Integer minPlayersPerTeam;

    @Min(1)
    private Integer maxPlayersPerTeam;

    /**
     * Optional: the event type is what the mobile app lists events by, and it still sends it. The admin panel does not
     * ask for it (what it calls the event type is the format) and the event gets the configured default type.
     */
    private UUID idEventType;

    /** Optional: the mobile app creates events without choosing a format. */
    private UUID idEventFormat;

    /** Optional: an event is private (seen only by its organization) unless the organizer makes it public. */
    private Boolean isPublic = false;

    /**
     * Optional: the complexes the event is played in. The calendar generator spreads the matches over their courts; an
     * event with none is scheduled on the single court of its schedule config, if it has one.
     */
    private List<UUID> sportComplexIds;
}
