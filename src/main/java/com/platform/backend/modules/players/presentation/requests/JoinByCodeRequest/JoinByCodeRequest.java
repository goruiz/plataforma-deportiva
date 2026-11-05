package com.platform.backend.modules.players.presentation.requests.JoinByCodeRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinByCodeRequest {

    /** As typed: spaces, hyphens and lower case are accepted, the service reads it leniently. */
    @NotBlank
    @Size(max = 32)
    private String code;
}
