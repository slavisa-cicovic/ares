package com.sc.ares.features.auth.controller;

import com.sc.ares.features.auth.dto.AuthTokenResponse;
import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import com.sc.ares.features.auth.dto.LoginRequest;
import com.sc.ares.features.auth.dto.LogoutRequest;
import com.sc.ares.features.auth.dto.RefreshTokenRequest;
import com.sc.ares.features.auth.service.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authApplicationService.login(request);
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authApplicationService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(request);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser() {
        return authApplicationService.getCurrentUser();
    }
}
