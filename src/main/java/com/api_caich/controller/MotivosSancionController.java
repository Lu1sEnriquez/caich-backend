package com.api_caich.controller;

import java.util.List;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.sancion.MotivoSancionDeleteResponseDTO;
import com.api_caich.dto.sancion.MotivoSancionRequestDTO;
import com.api_caich.dto.sancion.MotivoSancionResponseDTO;
import com.api_caich.service.MotivosSancionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/motivos-sancion")
@RequiredArgsConstructor
@Tag(name = "Motivos de Sanción", description = "APIs for motivos de sancion management")
@SecurityRequirement(name = "bearerAuth")
public class MotivosSancionController {
    private final MotivosSancionService motivosSancionService;

    @GetMapping
    @Operation(summary = "Get all motivos de sancion", description = "Get all motivos de sancion")
    public ResponseEntity<DataResponseDTO<List<MotivoSancionResponseDTO>>> getAllMotivosSancion() {
        return ResponseEntity.ok(DataResponseDTO.success(motivosSancionService.getAllMotivosSancion(),
                "Motivos de sancion retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new motivo de sancion", description = "Create a new motivo de sancion")
    public ResponseEntity<DataResponseDTO<MotivoSancionResponseDTO>> createMotivoSancion(
            @Valid @RequestBody MotivoSancionRequestDTO requestDTO) {
        return ResponseEntity.ok(DataResponseDTO.success(motivosSancionService.createMotivoSancion(requestDTO),
                "Motivo de sancion created successfully"));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a motivo de sancion", description = "Update a motivo de sancion")
    public ResponseEntity<DataResponseDTO<MotivoSancionResponseDTO>> updateMotivoSancion(@PathVariable("id") Long id,
            @Valid @RequestBody MotivoSancionRequestDTO requestDTO) {
        return ResponseEntity.ok(DataResponseDTO.success(motivosSancionService.updateMotivoSancion(id, requestDTO),
                "Motivo de sancion updated successfully"));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a motivo de sancion", description = "Delete a motivo de sancion")
    public ResponseEntity<DataResponseDTO<MotivoSancionDeleteResponseDTO>> deleteMotivoSancion(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(DataResponseDTO.success(motivosSancionService.deleteMotivoSancion(id),
                "Motivo de sancion deleted successfully"));
    }

}
