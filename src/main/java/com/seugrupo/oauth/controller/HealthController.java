package com.seugrupo.oauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de health check na porta principal da aplicação (OAUTH_INTERNAL_API_PORT),
 * garantindo compatibilidade direta com os healthchecks do Docker Compose oficial e local
 * enquanto as métricas de telemetria/Prometheus rodam na porta dedicada (OAUTH_INTERNAL_METRICS_PORT).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
