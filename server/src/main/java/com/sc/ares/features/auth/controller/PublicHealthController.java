package com.sc.ares.features.auth.controller;

import com.sc.ares.features.auth.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/health")
public class PublicHealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "ares");
    }
}
