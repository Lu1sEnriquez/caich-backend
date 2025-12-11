package com.api_caich.controller;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.horario.ConfiguracionHorariosDTO;
import com.api_caich.dto.horario.CrearConfiguracionHorariosDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.ConfiguracionHorariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/configuracion-horarios")
@RequiredArgsConstructor
@Tag(name = "Schedule Configuration", description = "APIs for managing schedule configurations")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionHorariosController {

        private final ConfiguracionHorariosService configuracionHorariosService;

        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Get schedule configuration by date", description = "Get disabled hours configuration for specific date and space")
        public ResponseEntity<DataResponseDTO<ConfiguracionHorariosDTO>> obtenerConfiguracionPorFecha(
                        @Parameter(description = "Space ID") @RequestParam Long espacioId,
                        @Parameter(description = "Date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

                ConfiguracionHorariosDTO configuracion = configuracionHorariosService.obtenerConfiguracionPorFecha(
                                espacioId,
                                fecha);
                return ResponseEntity
                                .ok(DataResponseDTO.success(configuracion,
                                                "Schedule configuration retrieved successfully"));
        }

        @GetMapping("/espacio/{espacioId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Get schedule configurations for space", description = "Get all schedule configurations for specific space")
        public ResponseEntity<DataResponseDTO<List<ConfiguracionHorariosDTO>>> obtenerConfiguracionesPorEspacio(
                        @Parameter(description = "Space ID") @PathVariable Long espacioId) {

                List<ConfiguracionHorariosDTO> configuraciones = configuracionHorariosService
                                .obtenerConfiguracionesPorEspacio(espacioId);
                return ResponseEntity
                                .ok(DataResponseDTO.success(configuraciones,
                                                "Space schedule configurations retrieved successfully"));
        }

        @GetMapping("/rango-fechas")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Get configurations by date range", description = "Get schedule configurations within date range")
        public ResponseEntity<DataResponseDTO<List<ConfiguracionHorariosDTO>>> obtenerConfiguracionesPorRangoFechas(
                        @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

                List<ConfiguracionHorariosDTO> configuraciones = configuracionHorariosService
                                .obtenerConfiguracionesPorRangoFechas(fechaInicio, fechaFin);
                return ResponseEntity
                                .ok(DataResponseDTO.success(configuraciones,
                                                "Schedule configurations retrieved successfully"));
        }

        @GetMapping("/horarios-disponibles")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Get available time slots", description = "Get list of available time slots for specific space and date")
        public ResponseEntity<DataResponseDTO<List<String>>> obtenerHorariosDisponibles(
                        @Parameter(description = "Space ID") @RequestParam Long espacioId,
                        @Parameter(description = "Date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

                List<LocalTime> horarios = configuracionHorariosService.obtenerHorariosDisponibles(espacioId, fecha);
                List<String> horariosStr = horarios.stream()
                                .map(LocalTime::toString)
                                .collect(java.util.stream.Collectors.toList());

                return ResponseEntity.ok(
                                DataResponseDTO.success(horariosStr, "Available time slots retrieved successfully"));
        }

        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Save schedule configuration", description = "Save or update disabled hours configuration for specific date and space")
        public ResponseEntity<DataResponseDTO<ConfiguracionHorariosDTO>> guardarConfiguracion(
                        @Valid @RequestBody CrearConfiguracionHorariosDTO configuracionDTO,
                        @AuthenticationPrincipal CustomUserDetails authentication) {

                Long usuarioId = authentication.getId();
                ConfiguracionHorariosDTO configuracion = configuracionHorariosService.guardarConfiguracion(
                                configuracionDTO,
                                usuarioId);
                return ResponseEntity.ok(
                                DataResponseDTO.success(configuracion, "Schedule configuration saved successfully"));
        }

        @DeleteMapping("/{configId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Delete schedule configuration", description = "Delete specific schedule configuration")
        public ResponseEntity<DataResponseDTO<Void>> eliminarConfiguracion(
                        @Parameter(description = "Configuration ID") @PathVariable Long configId) {

                configuracionHorariosService.eliminarConfiguracion(configId);
                return ResponseEntity.ok(DataResponseDTO.success(null, "Schedule configuration deleted successfully"));
        }

        @DeleteMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Delete configurations by space and date", description = "Delete all schedule configurations for specific space and date")
        public ResponseEntity<DataResponseDTO<Void>> eliminarConfiguracionesPorEspacioYFecha(
                        @Parameter(description = "Space ID") @RequestParam Long espacioId,
                        @Parameter(description = "Date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

                configuracionHorariosService.eliminarConfiguracionesPorEspacioYFecha(espacioId, fecha);
                return ResponseEntity.ok(DataResponseDTO.success(null, "Schedule configurations deleted successfully"));
        }

        @GetMapping("/disponibilidad")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Check time slot availability", description = "Check if a specific time slot is available for booking")
        public ResponseEntity<DataResponseDTO<Boolean>> verificarDisponibilidad(
                        @Parameter(description = "Space ID") @RequestParam Long espacioId,
                        @Parameter(description = "Date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        @Parameter(description = "Start time (HH:mm)") @RequestParam String horaInicio,
                        @Parameter(description = "End time (HH:mm)") @RequestParam String horaFin) {

                boolean disponible = configuracionHorariosService.verificarDisponibilidad(espacioId, fecha, horaInicio,
                                horaFin);
                String mensaje = disponible ? "Time slot is available" : "Time slot is not available";
                return ResponseEntity.ok(DataResponseDTO.success(disponible, mensaje));
        }

        @GetMapping("/configuracion-especial")
        @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
        @Operation(summary = "Check special configuration", description = "Check if a space has special schedule configuration for specific date")
        public ResponseEntity<DataResponseDTO<Boolean>> tieneConfiguracionEspecial(
                        @Parameter(description = "Space ID") @RequestParam Long espacioId,
                        @Parameter(description = "Date (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

                boolean tieneConfiguracion = configuracionHorariosService.tieneConfiguracionEspecial(espacioId, fecha);

                String mensaje = tieneConfiguracion ? "El espacio tiene configuración especial para esta fecha"
                                : "El espacio no tiene configuración especial para esta fecha";

                return ResponseEntity.ok(DataResponseDTO.success(tieneConfiguracion, mensaje));
        }
}