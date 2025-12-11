package com.api_caich.controller;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "APIs for role management")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all roles", description = "Get all available roles in the system")
    public ResponseEntity<DataResponseDTO<List<String>>> getAllRoles() {
        List<String> roles = roleService.getAllRoleNames();
        return ResponseEntity.ok(DataResponseDTO.success(roles, "Roles retrieved successfully"));
    }

    @GetMapping("/registerable")
    @Operation(summary = "Get registerable roles", description = "Get roles available for user registration")
    public ResponseEntity<DataResponseDTO<List<String>>> getRegisterableRoles() {
        List<String> roles = roleService.getRegisterableRoles();
        return ResponseEntity.ok(DataResponseDTO.success(roles, "Registerable roles retrieved"));
    }

    @GetMapping("/authorities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all authorities", description = "Get all Spring Security authorities")
    public ResponseEntity<DataResponseDTO<List<String>>> getAllAuthorities() {
        List<String> authorities = roleService.getAllAuthorities();
        return ResponseEntity.ok(DataResponseDTO.success(authorities, "Authorities retrieved successfully"));
    }
}