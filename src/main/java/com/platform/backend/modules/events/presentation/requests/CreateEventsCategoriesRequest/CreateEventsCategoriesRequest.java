package com.platform.backend.modules.events.presentation.requests.CreateEventsCategoriesRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEventsCategoriesRequest {

    @NotNull
    private UUID eventId;

    @NotNull
    private UUID categoryId;
}
