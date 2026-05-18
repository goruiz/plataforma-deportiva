package com.platform.backend.modules.players.presentation.requests.UpdatePlayerRequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePlayerRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String profilePhotoUrl;

    @Size(max = 20)
    private String status;

    private UUID teamId;
}
