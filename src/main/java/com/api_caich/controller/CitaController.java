package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.cita.ActualizarCitaDTO;
import com.api_caich.dto.cita.CitaDTO;
import com.api_caich.dto.cita.CrearCitaDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "APIs for managing appointments")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get appointments with filters", description = "Get filtered appointments list")
    public ResponseEntity<DataResponseDTO<List<CitaDTO>>> obtenerCitas(
            @Parameter(description = "Space ID") @RequestParam(required = false) Long espacioId,
            @Parameter(description = "Therapist ID") @RequestParam(required = false) Long terapeutaId,
            @Parameter(description = "Patient ID") @RequestParam(required = false) Long pacienteId,
            @Parameter(description = "Appointment status") @RequestParam(required = false) String estado) {

        List<CitaDTO> citas = citaService.obtenerCitasConFiltros(espacioId, terapeutaId, pacienteId, estado);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Appointments retrieved successfully"));
    }

    @GetMapping("/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get appointment by ID", description = "Get appointment details by ID")
    public ResponseEntity<DataResponseDTO<CitaDTO>> obtenerCitaPorId(
            @Parameter(description = "Appointment ID") @PathVariable Long citaId) {

        CitaDTO cita = citaService.obtenerCitaPorId(citaId);
        return ResponseEntity.ok(DataResponseDTO.success(cita, "Appointment retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Create new appointment", description = "Create a new appointment with space availability validation")
    public ResponseEntity<DataResponseDTO<CitaDTO>> crearCita(
            @Valid @RequestBody CrearCitaDTO crearCitaDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        CitaDTO cita = citaService.crearCita(crearCitaDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(cita, "Appointment created successfully"));
    }

    @PutMapping("/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update appointment", description = "Update appointment details")
    public ResponseEntity<DataResponseDTO<CitaDTO>> actualizarCita(
            @Parameter(description = "Appointment ID") @PathVariable Long citaId,
            @Valid @RequestBody ActualizarCitaDTO actualizarCitaDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        CitaDTO cita = citaService.actualizarCita(citaId, actualizarCitaDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(cita, "Appointment updated successfully"));
    }

    @DeleteMapping("/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Delete appointment", description = "Delete appointment (only if in 'Agendada' status)")
    public ResponseEntity<BaseResponseDTO> eliminarCita(
            @Parameter(description = "Appointment ID") @PathVariable Long citaId,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        citaService.eliminarCita(citaId, usuarioId);
        return ResponseEntity.ok(BaseResponseDTO.success("Appointment deleted successfully"));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get appointments by patient", description = "Get appointments list for specific patient")
    public ResponseEntity<DataResponseDTO<List<CitaDTO>>> obtenerCitasPorPaciente(
            @Parameter(description = "Patient ID") @PathVariable Long pacienteId,
            @Parameter(description = "Appointment status") @RequestParam(required = false) String estado) {

        List<CitaDTO> citas = citaService.obtenerCitasPorPaciente(pacienteId, estado);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Patient appointments retrieved successfully"));
    }

    @GetMapping("/terapeuta/{terapeutaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get appointments by therapist", description = "Get appointments list for specific therapist")
    public ResponseEntity<DataResponseDTO<List<CitaDTO>>> obtenerCitasPorTerapeuta(
            @Parameter(description = "Therapist ID") @PathVariable Long terapeutaId,
            @Parameter(description = "Appointment status") @RequestParam(required = false) String estado) {

        List<CitaDTO> citas = citaService.obtenerCitasPorTerapeuta(terapeutaId, estado);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Therapist appointments retrieved successfully"));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get appointments by status", description = "Get appointments list by specific status")
    public ResponseEntity<DataResponseDTO<List<CitaDTO>>> obtenerCitasPorEstado(
            @Parameter(description = "Appointment status") @PathVariable String estado) {

        List<CitaDTO> citas = citaService.obtenerCitasPorEstado(estado);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Appointments by status retrieved successfully"));
    }
}