package com.platform.backend.modules.events.presentation.requests.CreateEventRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEventRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String type;

    @NotBlank
    @Size(max = 30)
    private String format;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;
}
