package com.platform.backend.modules.events.presentation.responses.EventResponse;

import com.platform.backend.modules.events.presentation.responses.EventTypeResponse.EventTypeResponse;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventResponse {

    private UUID id;
    private String name;
    private String description;
    private String format;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer minPlayersPerTeam;
    private Integer maxPlayersPerTeam;
    private EventTypeResponse eventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
