package com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SportComplexResponse {

    private UUID id;
    private String name;
    private String description;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
