package com.platform.backend.modules.events.presentation.requests.UpdateEventRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateEventRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Integer minPlayersPerTeam;

    @Min(1)
    private Integer maxPlayersPerTeam;

    private UUID idEventType;

    /** Null leaves the current format untouched, so a format already set can be changed but not cleared. */
    private UUID idEventFormat;

    /** Null leaves the event as it is; false makes a public event private again. */
    private Boolean isPublic;

    /**
     * Null leaves the complexes as they are; a list replaces them (an empty one takes the event out of every complex).
     * Matches already generated keep their courts: only the next calendar uses the new complexes.
     */
    private List<UUID> sportComplexIds;
}
