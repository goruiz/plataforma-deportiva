package com.platform.backend.modules.users.presentation.controllers;

import com.platform.backend.modules.users.presentation.requests.RegisterRequest.RegisterRequest;
import com.platform.backend.modules.users.presentation.responses.RegisterResponse.RegisterResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.platform.backend.modules.users.application.iservices.IAuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final IAuthService iAuthSerivce;

    public AuthController(IAuthService iAuthService) {
        this.iAuthSerivce = iAuthService;
    }

    @PostMapping("/login")
    public void login() {

    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest registerRequest) {
        return iAuthSerivce.register(registerRequest);
    }
}
