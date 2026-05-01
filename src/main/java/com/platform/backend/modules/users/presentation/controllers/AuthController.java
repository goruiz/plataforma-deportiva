package com.platform.backend.modules.users.presentation.controllers;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController
{
    @PostMapping("/login")
    public void login(){

    }
    @PostMapping("/register")
    public void register(){

    }
}
