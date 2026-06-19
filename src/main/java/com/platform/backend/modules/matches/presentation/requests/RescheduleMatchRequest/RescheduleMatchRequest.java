package com.platform.backend.modules.matches.presentation.requests.RescheduleMatchRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleMatchRequest {

    @NotNull
    private LocalDateTime newDate;
}
