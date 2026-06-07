package com.platform.backend.modules.events.presentation.requests.UpdateEventRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateEventRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 30)
    private String format;

    @Size(max = 20)
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Integer minPlayersPerTeam;

    @Min(1)
    private Integer maxPlayersPerTeam;

    private UUID idEventType;
}
