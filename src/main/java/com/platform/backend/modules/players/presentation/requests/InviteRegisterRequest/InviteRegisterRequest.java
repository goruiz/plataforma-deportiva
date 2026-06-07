package com.platform.backend.modules.players.presentation.requests.InviteRegisterRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class InviteRegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotNull
    private UUID teamId;
}
