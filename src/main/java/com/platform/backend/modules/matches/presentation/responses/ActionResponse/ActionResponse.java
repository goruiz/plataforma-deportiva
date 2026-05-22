package com.platform.backend.modules.matches.presentation.responses.ActionResponse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ActionResponse {

    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
