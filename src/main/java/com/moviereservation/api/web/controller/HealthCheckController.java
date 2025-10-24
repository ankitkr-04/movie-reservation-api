package com.moviereservation.api.web.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moviereservation.api.constant.Route;
import com.moviereservation.api.web.dto.response.wrappers.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Health check endpoints for monitoring system status.
 * Provides detailed health information beyond standard actuator health.
 */
@RestController
@RequestMapping(Route.API_V1)
@Tag(name = "Health Check", description = "System health and status endpoints")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthIndicator dbHealthIndicator;

    /**
     * Simple health check endpoint.
     * Returns OK if application is running.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Quick health check endpoint. Returns 200 OK if application is running.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {

        final Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "Movie Reservation API");
        health.put("version", "1.0.0");

        return ResponseEntity.ok(ApiResponse.success("Service is healthy", health));
    }

    /**
     * Detailed health check with component status.
     * Checks database connectivity and other critical components.
     */
    @GetMapping("/health/detailed")
    @Operation(summary = "Detailed health check", description = "Comprehensive health check including database connectivity and system metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {

        final Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "Movie Reservation API");
        health.put("version", "1.0.0");

        // Database health
        final Health dbHealth = dbHealthIndicator.health();
        final Map<String, Object> database = new HashMap<>();
        database.put("status", dbHealth.getStatus().getCode());
        database.put("details", dbHealth.getDetails());
        health.put("database", database);

        // Runtime information
        final Runtime runtime = Runtime.getRuntime();
        final Map<String, Object> system = new HashMap<>();
        system.put("processors", runtime.availableProcessors());
        system.put("freeMemory", runtime.freeMemory());
        system.put("totalMemory", runtime.totalMemory());
        system.put("maxMemory", runtime.maxMemory());
        health.put("system", system);

        // Overall status based on components
        final boolean allHealthy = "UP".equals(database.get("status"));
        health.put("status", allHealthy ? "UP" : "DEGRADED");

        return ResponseEntity.ok(
                ApiResponse.success("Health check completed", health));
    }

    /**
     * Readiness probe for Kubernetes/container orchestration.
     * Indicates if application is ready to serve traffic.
     */
    @GetMapping("/health/ready")
    @Operation(summary = "Readiness probe", description = "Readiness probe endpoint for container orchestration. Returns 200 when ready to serve traffic.")
    public ResponseEntity<ApiResponse<String>> readinessProbe() {
        // Check if database is accessible
        final Health dbHealth = dbHealthIndicator.health();

        if (!"UP".equals(dbHealth.getStatus().getCode())) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Service not ready - database unavailable"));
        }

        return ResponseEntity.ok(ApiResponse.success("Service is ready"));
    }

    /**
     * Liveness probe for Kubernetes/container orchestration.
     * Indicates if application is alive and should not be restarted.
     */
    @GetMapping("/health/live")
    @Operation(summary = "Liveness probe", description = "Liveness probe endpoint for container orchestration. Returns 200 if application is alive.")
    public ResponseEntity<ApiResponse<String>> livenessProbe() {
        return ResponseEntity.ok(ApiResponse.success("Service is alive"));
    }
}
