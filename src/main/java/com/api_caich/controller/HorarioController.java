package com.api_caich.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

import com.api_caich.dto.horario.DisponibilidadDTO;
import com.api_caich.dto.horario.ExcepcionHorarioDTO;
import com.api_caich.dto.horario.HorarioBaseDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.DisponibilidadService;
import com.api_caich.service.ExcepcionHorarioService;
import com.api_caich.service.HorarioBaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para gestionar horarios y disponibilidad.
 */
@Slf4j
@RestController
@RequestMapping("/horarios")
@RequiredArgsConstructor
@Tag(name = "Horarios", description = "Gestión de horarios base, excepciones y disponibilidad")
public class HorarioController {

    private final HorarioBaseService horarioBaseService;
    private final ExcepcionHorarioService excepcionHorarioService;
    private final DisponibilidadService disponibilidadService;

    // ==================== HORARIO BASE ====================

    /**
     * Obtiene el horario base para un espacio específico.
     */
    @GetMapping("/base/espacio/{espacioId}")
    @Operation(summary = "Obtener horario base de un espacio")
    public ResponseEntity<HorarioBaseDTO> obtenerHorarioBase(
            @PathVariable Long espacioId) {
        HorarioBaseDTO dto = horarioBaseService.obtenerHorarioBase(espacioId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtiene el horario base global.
     */
    @GetMapping("/base/global")
    @Operation(summary = "Obtener horario base global")
    public ResponseEntity<HorarioBaseDTO> obtenerHorarioBaseGlobal() {
        HorarioBaseDTO dto = horarioBaseService.obtenerHorarioBaseGlobal();
        return ResponseEntity.ok(dto);
    }

    /**
     * Crea o actualiza el horario base para un espacio.
     */
    @PostMapping("/base/espacio/{espacioId}")
    @Operation(summary = "Crear o actualizar horario base de un espacio")
    public ResponseEntity<HorarioBaseDTO> crearOActualizarHorarioBase(
            @PathVariable Long espacioId,
            @Valid @RequestBody HorarioBaseDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        HorarioBaseDTO result = horarioBaseService.crearOActualizar(espacioId, dto, usuarioId);
        return ResponseEntity.ok(result);
    }

    /**
     * Crea el horario base global.
     */
    @PostMapping("/base/global")
    @Operation(summary = "Crear horario base global")
    public ResponseEntity<HorarioBaseDTO> crearHorarioBaseGlobal(
            @Valid @RequestBody HorarioBaseDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        HorarioBaseDTO result = horarioBaseService.crearGlobal(dto, usuarioId);
        return ResponseEntity.ok(result);
    }

    /**
     * Desactiva el horario base de un espacio.
     */
    @DeleteMapping("/base/espacio/{espacioId}")
    @Operation(summary = "Desactivar horario base de un espacio")
    public ResponseEntity<Void> desactivarHorarioBase(
            @PathVariable Long espacioId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        horarioBaseService.desactivar(espacioId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    // ==================== EXCEPCIONES ====================

    /**
     * Obtiene una excepción específica.
     */
    @GetMapping("/excepciones/{excepcionId}")
    @Operation(summary = "Obtener una excepción horaria")
    public ResponseEntity<ExcepcionHorarioDTO> obtenerExcepcion(@PathVariable Long excepcionId) {
        ExcepcionHorarioDTO dto = excepcionHorarioService.obtener(excepcionId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtiene todas las excepciones para un espacio en un rango de fechas.
     */
    @GetMapping("/excepciones/espacio/{espacioId}")
    @Operation(summary = "Obtener excepciones de un espacio en un rango de fechas")
    public ResponseEntity<?> obtenerExcepcionesPorEspacio(
            @PathVariable Long espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        var excepciones = excepcionHorarioService.obtenerPorEspacioEnRango(espacioId, fechaInicio, fechaFin);
        return ResponseEntity.ok(excepciones);
    }

    /**
     * Obtiene todas las excepciones globales en un rango de fechas.
     */
    @GetMapping("/excepciones/globales")
    @Operation(summary = "Obtener excepciones globales en un rango de fechas")
    public ResponseEntity<?> obtenerExcepcionesGlobales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        var excepciones = excepcionHorarioService.obtenerExcepcionesGlobalesEnRango(fechaInicio, fechaFin);
        return ResponseEntity.ok(excepciones);
    }

    /**
     * Crea una nueva excepción horaria.
     */
    @PostMapping("/excepciones")
    @Operation(summary = "Crear una excepción horaria (bloqueo, festivo, mantenimiento)")
    public ResponseEntity<ExcepcionHorarioDTO> crearExcepcion(
            @Valid @RequestBody ExcepcionHorarioDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        ExcepcionHorarioDTO result = excepcionHorarioService.crear(dto, usuarioId);
        return ResponseEntity.ok(result);
    }

    /**
     * Actualiza una excepción horaria.
     */
    @PutMapping("/excepciones/{excepcionId}")
    @Operation(summary = "Actualizar una excepción horaria")
    public ResponseEntity<ExcepcionHorarioDTO> actualizarExcepcion(
            @PathVariable Long excepcionId,
            @Valid @RequestBody ExcepcionHorarioDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        ExcepcionHorarioDTO result = excepcionHorarioService.actualizar(excepcionId, dto, usuarioId);
        return ResponseEntity.ok(result);
    }

    /**
     * Elimina (desactiva) una excepción horaria.
     */
    @DeleteMapping("/excepciones/{excepcionId}")
    @Operation(summary = "Eliminar (desactivar) una excepción horaria")
    public ResponseEntity<Void> eliminarExcepcion(
            @PathVariable Long excepcionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        excepcionHorarioService.eliminar(excepcionId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    // ==================== DISPONIBILIDAD ====================

    /**
     * Obtiene los slots disponibles para un espacio en una fecha específica.
     * Este endpoint es público y usado por el cliente para mostrar disponibilidad.
     */
    @GetMapping("/disponibilidad/espacio/{espacioId}")
    @Operation(summary = "Obtener slots disponibles para un espacio en una fecha")
    public ResponseEntity<DisponibilidadDTO> obtenerDisponibilidad(
            @PathVariable Long espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long ticketIdToExclude) {
        DisponibilidadDTO dto = disponibilidadService.obtenerSlotsDisponibles(espacioId, fecha, ticketIdToExclude);
        return ResponseEntity.ok(dto);
    }

    /**
     * Verifica si una hora específica está disponible (validación rápida).
     */
    @GetMapping("/disponibilidad/espacio/{espacioId}/validar")
    @Operation(summary = "Validar si una hora está disponible")
    public ResponseEntity<Boolean> validarDisponibilidad(
            @PathVariable Long espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaFin,
            @RequestParam(required = false) Long ticketIdToExclude) {
        boolean disponible = disponibilidadService.estaDisponible(espacioId, horaInicio, horaFin, ticketIdToExclude);
        return ResponseEntity.ok(disponible);
    }
}
