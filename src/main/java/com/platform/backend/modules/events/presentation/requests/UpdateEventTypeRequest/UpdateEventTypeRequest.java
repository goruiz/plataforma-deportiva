package com.platform.backend.modules.events.presentation.requests.UpdateEventTypeRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEventTypeRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;
}
