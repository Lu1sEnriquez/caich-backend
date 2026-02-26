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
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.cita.ActualizarCitaRequestDTO;
import com.api_caich.dto.cita.CitaResponseDTO;
import com.api_caich.dto.cita.CrearCitaRequestDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.CitaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador para la API de Citas.
 * El frontend usa /citas para crear/actualizar/eliminar citas desde el calendario.
 * Internamente delega al TicketService mediante el CitaService puente.
 */
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "APIs para gestión de citas terapéuticas")
@SecurityRequirement(name = "bearerAuth")
public class CitasController {

    private final CitaService citaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Crear nueva cita", description = "Crea un ticket con componente cita desde el calendario")
    public ResponseEntity<DataResponseDTO<CitaResponseDTO>> crearCita(
            @Valid @RequestBody CrearCitaRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CitaResponseDTO cita = citaService.crearCita(request, userDetails.getId());
        return ResponseEntity.ok(DataResponseDTO.success(cita, "Cita creada exitosamente"));
    }

    @PutMapping("/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Actualizar cita", description = "Actualiza datos de una cita existente")
    public ResponseEntity<DataResponseDTO<CitaResponseDTO>> actualizarCita(
            @PathVariable Long citaId,
            @RequestBody ActualizarCitaRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CitaResponseDTO cita = citaService.actualizarCita(citaId, request, userDetails.getId());
        return ResponseEntity.ok(DataResponseDTO.success(cita, "Cita actualizada exitosamente"));
    }

    @DeleteMapping("/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Eliminar cita", description = "Cancela el ticket asociado a la cita")
    public ResponseEntity<BaseResponseDTO> eliminarCita(
            @PathVariable Long citaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        citaService.eliminarCita(citaId, userDetails.getId());
        return ResponseEntity.ok(BaseResponseDTO.success("Cita eliminada exitosamente"));
    }

    @GetMapping("/terapeuta/{terapeutaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Citas por terapeuta", description = "Obtiene citas asignadas a un terapeuta")
    public ResponseEntity<DataResponseDTO<List<CitaResponseDTO>>> obtenerCitasPorTerapeuta(
            @PathVariable Long terapeutaId) {

        List<CitaResponseDTO> citas = citaService.obtenerCitasPorTerapeuta(terapeutaId);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Citas del terapeuta recuperadas"));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Citas por paciente", description = "Obtiene citas de un paciente")
    public ResponseEntity<DataResponseDTO<List<CitaResponseDTO>>> obtenerCitasPorPaciente(
            @PathVariable Long pacienteId) {

        List<CitaResponseDTO> citas = citaService.obtenerCitasPorPaciente(pacienteId);
        return ResponseEntity.ok(DataResponseDTO.success(citas, "Citas del paciente recuperadas"));
    }
}
