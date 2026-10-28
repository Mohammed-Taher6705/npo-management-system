package com.npo.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Setup check only: GET /api/health -> backend and MongoDB status. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        boolean db = DatabaseConnection.getInstance().ping();
        return Map.of("backend", "UP", "mongodb", db ? "UP" : "DOWN");
    }
}
