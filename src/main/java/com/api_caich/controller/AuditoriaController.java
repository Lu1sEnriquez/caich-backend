package com.api_caich.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.auditoria.HistorialCambioDTO;
import com.api_caich.service.AuditoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "APIs for audit trail and change history")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/entidad/{entidad}/{entidadId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get entity audit trail", description = "Get audit history for specific entity")
    public ResponseEntity<DataResponseDTO<List<HistorialCambioDTO>>> obtenerHistorialEntidad(
            @PathVariable String entidad,
            @PathVariable Long entidadId) {

        List<HistorialCambioDTO> historial = auditoriaService.obtenerHistorial(entidad, entidadId);
        return ResponseEntity.ok(DataResponseDTO.success(historial, "Audit trail retrieved"));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user audit trail", description = "Get audit history for specific user")
    public ResponseEntity<DataResponseDTO<List<HistorialCambioDTO>>> obtenerHistorialUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "50") int limite) {

        List<HistorialCambioDTO> historial = auditoriaService.obtenerHistorialPorUsuario(usuarioId, limite);
        return ResponseEntity.ok(DataResponseDTO.success(historial, "User audit trail retrieved"));
    }
}