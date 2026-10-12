package com.platform.backend.modules.events.presentation.requests.UpdateEventFormatRequest;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEventFormatRequest {

    // Null means "leave unchanged"; a value, when sent, can't be blank.
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;
}
