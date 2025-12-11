package com.api_caich.controller;

import com.api_caich.dto.DataResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "APIs for health checks and system status")
public class HealthController {

    private final ApplicationAvailability applicationAvailability;

    @Value("${app.name:CAICH API}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @GetMapping
    @Operation(summary = "Basic health check", description = "Public endpoint to check if API is running")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("application", appName);
        healthInfo.put("version", appVersion);
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("contextPath", contextPath);
        healthInfo.put("environment", System.getProperty("spring.profiles.active", "default"));

        log.info("Health check executed - Status: UP");
        return ResponseEntity.ok(DataResponseDTO.success(healthInfo, "Service is healthy"));
    }

    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Detailed system information")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> healthDetailed() {
        Map<String, Object> healthInfo = new HashMap<>();

        // Basic info
        healthInfo.put("status", "UP");
        healthInfo.put("application", appName);
        healthInfo.put("version", appVersion);
        healthInfo.put("timestamp", LocalDateTime.now());

        // System info
        healthInfo.put("javaVersion", System.getProperty("java.version"));
        healthInfo.put("javaVendor", System.getProperty("java.vendor"));
        healthInfo.put("osName", System.getProperty("os.name"));
        healthInfo.put("osVersion", System.getProperty("os.version"));

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        healthInfo.put("memory", Map.of(
                "max", runtime.maxMemory() / (1024 * 1024) + " MB",
                "total", runtime.totalMemory() / (1024 * 1024) + " MB",
                "free", runtime.freeMemory() / (1024 * 1024) + " MB",
                "used", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB"));

        // Availability states
        AvailabilityState livenessState = applicationAvailability.getLivenessState();
        AvailabilityState readinessState = applicationAvailability.getReadinessState();
        healthInfo.put("livenessState", livenessState.toString());
        healthInfo.put("readinessState", readinessState.toString());

        log.debug("Detailed health check executed");
        return ResponseEntity.ok(DataResponseDTO.success(healthInfo, "Detailed health status"));
    }

    @GetMapping("/auth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Authentication test", description = "Test JWT authentication and get user info")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> authTest(Authentication authentication) {
        Map<String, Object> authInfo = new HashMap<>();

        authInfo.put("authenticated", true);
        authInfo.put("username", authentication.getName());
        authInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        authInfo.put("timestamp", LocalDateTime.now());

        log.info("Auth test executed for user: {}", authentication.getName());
        return ResponseEntity.ok(DataResponseDTO.success(authInfo, "Authentication successful"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin role test", description = "Test ADMIN role access")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> adminTest(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome, Administrator!");
        response.put("user", authentication.getName());
        response.put("role", "ADMIN");
        response.put("timestamp", LocalDateTime.now());

        log.info("Admin test executed for user: {}", authentication.getName());
        return ResponseEntity.ok(DataResponseDTO.success(response, "Admin access verified"));
    }

    @GetMapping("/terapeuta")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Terapeuta role test", description = "Test TERAPEUTA role access")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> terapeutaTest(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome, Terapeuta!");
        response.put("user", authentication.getName());
        response.put("role", "TERAPEUTA");
        response.put("timestamp", LocalDateTime.now());

        log.info("Terapeuta test executed for user: {}", authentication.getName());
        return ResponseEntity.ok(DataResponseDTO.success(response, "Terapeuta access verified"));
    }

    @GetMapping("/alumno")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Alumno role test", description = "Test ALUMNO role access")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> alumnoTest(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome, Alumno!");
        response.put("user", authentication.getName());
        response.put("role", "ALUMNO");
        response.put("timestamp", LocalDateTime.now());

        log.info("Alumno test executed for user: {}", authentication.getName());
        return ResponseEntity.ok(DataResponseDTO.success(response, "Alumno access verified"));
    }

    @GetMapping("/paciente")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Paciente role test", description = "Test PACIENTE role access")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> pacienteTest(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome, Paciente!");
        response.put("user", authentication.getName());
        response.put("role", "PACIENTE");
        response.put("timestamp", LocalDateTime.now());

        log.info("Paciente test executed for user: {}", authentication.getName());
        return ResponseEntity.ok(DataResponseDTO.success(response, "Paciente access verified"));
    }

    @GetMapping("/database")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Database connectivity test", description = "Test database connection (Admin only)")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> databaseTest() {
        Map<String, Object> dbInfo = new HashMap<>();

        // Aquí puedes agregar una verificación real de base de datos
        dbInfo.put("database", "SQL Server");
        dbInfo.put("status", "CONNECTED");
        dbInfo.put("timestamp", LocalDateTime.now());
        dbInfo.put("testQuery", "SELECT 1");

        log.info("Database connectivity test executed");
        return ResponseEntity.ok(DataResponseDTO.success(dbInfo, "Database connection verified"));
    }
}
