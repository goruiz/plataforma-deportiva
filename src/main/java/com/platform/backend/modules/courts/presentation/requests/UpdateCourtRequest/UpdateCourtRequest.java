package com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCourtRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 255)
    private String location;
}
