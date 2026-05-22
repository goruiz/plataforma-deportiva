package com.platform.backend.modules.courts.presentation.responses.CourtResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CourtResponse {

    private UUID id;
    private String name;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
