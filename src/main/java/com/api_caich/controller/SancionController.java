package com.api_caich.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.sancion.CrearSancionDTO;
import com.api_caich.dto.sancion.SancionDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.SancionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sanciones")
@RequiredArgsConstructor
@Tag(name = "Sanciones", description = "APIs for student sanctions management")
@SecurityRequirement(name = "bearerAuth")
public class SancionController {

    private final SancionService sancionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create sanction", description = "Create a new sanction for a student")
    public ResponseEntity<DataResponseDTO<SancionDTO>> crearSancion(
            @Valid @RequestBody CrearSancionDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SancionDTO sancion = sancionService.crearSancion(request, userDetails.getId());

        return ResponseEntity.ok(DataResponseDTO.success(sancion, "Sanción creada exitosamente"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all sanctions", description = "Get list of all sanctions")
    public ResponseEntity<DataResponseDTO<List<SancionDTO>>> obtenerTodasSanciones(
            @RequestParam(required = false) String estado) {

        List<SancionDTO> sanciones = sancionService.obtenerTodasSanciones(estado);

        return ResponseEntity.ok(DataResponseDTO.success(sanciones, "Sanciones obtenidas exitosamente"));
    }

    @GetMapping("/alumno/{alumnoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get student sanctions", description = "Get all sanctions for a specific student")
    public ResponseEntity<DataResponseDTO<List<SancionDTO>>> obtenerSancionesAlumno(
            @Parameter(description = "Student ID") @PathVariable Long alumnoId) {

        List<SancionDTO> sanciones = sancionService.obtenerSancionesAlumno(alumnoId);

        return ResponseEntity.ok(DataResponseDTO.success(sanciones, "Sanciones del alumno obtenidas"));
    }

    @GetMapping("/{sancionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get sanction by ID", description = "Get details of a specific sanction")
    public ResponseEntity<DataResponseDTO<SancionDTO>> obtenerSancion(
            @Parameter(description = "Sanction ID") @PathVariable Long sancionId) {

        SancionDTO sancion = sancionService.obtenerSancion(sancionId);

        return ResponseEntity.ok(DataResponseDTO.success(sancion, "Sanción obtenida exitosamente"));
    }

    @PutMapping("/{sancionId}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel sanction", description = "Cancel an active sanction")
    public ResponseEntity<DataResponseDTO<SancionDTO>> anularSancion(
            @Parameter(description = "Sanction ID") @PathVariable Long sancionId,
            @RequestParam String motivoAnulacion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SancionDTO sancion = sancionService.anularSancion(
                sancionId, motivoAnulacion, userDetails.getId());

        return ResponseEntity.ok(DataResponseDTO.success(sancion, "Sanción anulada exitosamente"));
    }

    @PutMapping("/{sancionId}/cumplir")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete sanction", description = "Mark an active sanction as completed/served")
    public ResponseEntity<DataResponseDTO<SancionDTO>> cumplirSancion(
            @Parameter(description = "Sanction ID") @PathVariable Long sancionId) {

        SancionDTO sancion = sancionService.cumplirSancion(sancionId);

        return ResponseEntity.ok(DataResponseDTO.success(sancion, "Sanción marcada como cumplida"));
    }

    @DeleteMapping("/{sancionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete sanction", description = "Permanently delete a sanction")
    public ResponseEntity<DataResponseDTO<Void>> eliminarSancion(
            @Parameter(description = "Sanction ID") @PathVariable Long sancionId) {

        sancionService.eliminarSancion(sancionId);

        return ResponseEntity.ok(DataResponseDTO.success(null, "Sanción eliminada exitosamente"));
    }
}
