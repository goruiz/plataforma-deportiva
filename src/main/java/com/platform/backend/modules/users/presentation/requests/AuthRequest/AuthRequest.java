package com.platform.backend.modules.users.presentation.requests.AuthRequest;
import jakarta.validation.constraints.NotBlank;


public class AuthRequest {

    @NotBlank
    private String email;
    @NotBlank
    private String password;



}
