package com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCourtRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String location;

    private UUID idSportComplex;
}
