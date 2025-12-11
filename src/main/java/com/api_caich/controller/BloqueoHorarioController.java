package com.api_caich.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.bloqueo.BloqueoHorarioDTO;
import com.api_caich.dto.bloqueo.CrearBloqueoHorarioDTO;
import com.api_caich.entity.BloqueoHorario;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.BloqueoHorarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/bloqueos-horarios")
@RequiredArgsConstructor
@Tag(name = "Schedule Blocks", description = "APIs for managing schedule blocks")
@SecurityRequirement(name = "bearerAuth")
public class BloqueoHorarioController {

    private final BloqueoHorarioService bloqueoHorarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get all schedule blocks", description = "Get list of all schedule blocks")
    public ResponseEntity<DataResponseDTO<List<BloqueoHorarioDTO>>> obtenerTodosBloqueos() {
        List<BloqueoHorario> bloqueos = bloqueoHorarioService.obtenerTodosBloqueos();
        List<BloqueoHorarioDTO> bloqueosDTO = bloqueos.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponseDTO.success(bloqueosDTO, "Schedule blocks retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Create schedule block", description = "Create a new schedule block for specific dates")
    public ResponseEntity<DataResponseDTO<BloqueoHorarioDTO>> crearBloqueo(
            @Valid @RequestBody CrearBloqueoHorarioDTO crearBloqueoDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        BloqueoHorario bloqueo = bloqueoHorarioService.crearBloqueo(crearBloqueoDTO, usuarioId);
        BloqueoHorarioDTO bloqueoDTO = mapToDTO(bloqueo);

        return ResponseEntity.ok(DataResponseDTO.success(bloqueoDTO, "Schedule block created successfully"));
    }

    @DeleteMapping("/{bloqueoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Delete schedule block", description = "Delete specific schedule block")
    public ResponseEntity<BaseResponseDTO> eliminarBloqueo(
            @Parameter(description = "Block ID") @PathVariable Long bloqueoId,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        bloqueoHorarioService.eliminarBloqueo(bloqueoId, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Schedule block deleted successfully"));
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Check availability considering blocks", description = "Check if a specific datetime is available considering schedule blocks")
    public ResponseEntity<DataResponseDTO<Boolean>> verificarDisponibilidad(
            @Parameter(description = "Space ID") @RequestParam Long espacioId,
            @Parameter(description = "Date and time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {

        boolean disponible = bloqueoHorarioService.verificarDisponibilidad(espacioId, fechaHora);
        String mensaje = disponible ? "DateTime is available" : "DateTime is blocked";

        return ResponseEntity.ok(DataResponseDTO.success(disponible, mensaje));
    }

    private BloqueoHorarioDTO mapToDTO(BloqueoHorario bloqueo) {
        return BloqueoHorarioDTO.builder()
            .bloqueoId(bloqueo.getBloqueoId())
            .espacioId(bloqueo.getEspacio() != null ? bloqueo.getEspacio().getEspacioId() : null)
            .espacioNombre(bloqueo.getEspacio() != null ? bloqueo.getEspacio().getNombre() : "GLOBAL")
            .fechaInicio(bloqueo.getFechaInicio())
            .fechaFin(bloqueo.getFechaFin())
            .motivo(bloqueo.getMotivo())
            .todoElDia(bloqueo.getTodoElDia())
            .creadoPor(bloqueo.getCreadoPor() != null ? bloqueo.getCreadoPor().getNombreCompleto() : null)
            .fechaCreacion(bloqueo.getFechaCreacion())
            .build();
    }
}