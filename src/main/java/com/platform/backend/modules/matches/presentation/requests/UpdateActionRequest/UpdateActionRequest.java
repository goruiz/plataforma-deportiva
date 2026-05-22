package com.platform.backend.modules.matches.presentation.requests.UpdateActionRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateActionRequest {

    @Size(max = 255)
    private String name;
}
