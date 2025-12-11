package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.LoginRequestDTO;
import com.api_caich.dto.LoginResponseDTO;
import com.api_caich.dto.LogoutRequestDTO;
import com.api_caich.dto.RefreshTokenRequestDTO;
import com.api_caich.dto.RegisterRequestDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<DataResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(DataResponseDTO.success(response, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register new user (Terapeuta, Alumno or Paciente)")
    public ResponseEntity<DataResponseDTO<LoginResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO registerRequest) {
        LoginResponseDTO response = authService.register(registerRequest);
        return ResponseEntity.ok(DataResponseDTO.success(response, "Registration successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<DataResponseDTO<LoginResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
        LoginResponseDTO response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(DataResponseDTO.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout user", description = "Logout user and invalidate refresh token")
    public ResponseEntity<BaseResponseDTO> logout(
            @Valid @RequestBody LogoutRequestDTO logoutRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logout(logoutRequest.getRefreshToken());

        return ResponseEntity.ok(BaseResponseDTO.success("Sesión cerrada exitosamente"));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout all sessions", description = "Logout user from all devices")
    public ResponseEntity<BaseResponseDTO> logoutAll(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        authService.logoutAllSessions(usuarioId);
        return ResponseEntity.ok(BaseResponseDTO.success("Logged out from all devices"));
    }
}
