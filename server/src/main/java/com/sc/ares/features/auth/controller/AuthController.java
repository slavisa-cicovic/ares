package com.sc.ares.features.auth.controller;

import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import com.sc.ares.features.auth.dto.LoginRequest;
import com.sc.ares.features.auth.service.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(@Valid @RequestBody LoginRequest request) {
        return authApplicationService.login(request);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser() {
        return authApplicationService.getCurrentUser();
    }
}
