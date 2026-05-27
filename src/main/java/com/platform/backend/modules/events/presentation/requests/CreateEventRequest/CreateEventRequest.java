package com.platform.backend.modules.events.presentation.requests.CreateEventRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateEventRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 30)
    private String format;

    @Size(max = 20)
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    private UUID idEventType;
}
