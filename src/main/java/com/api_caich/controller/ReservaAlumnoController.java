package com.api_caich.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.horario.DisponibilidadDTO;
import com.api_caich.dto.reserva.CrearReservaAlumnoDTO;
import com.api_caich.dto.reserva.MisReservasDTO;
import com.api_caich.dto.reserva.ReservaAlumnoResponseDTO;
import com.api_caich.dto.usuario.UsuarioDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.ReservaAlumnoService;
import com.api_caich.service.UsuarioService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservas/alumno")
@RequiredArgsConstructor
@Tag(name = "Reservas Alumno", description = "APIs for student cubicle reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservaAlumnoController {

    private final ReservaAlumnoService reservaAlumnoService;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/con-monedero", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Create student reservation paying with wallet",
               description = "Create a confirmed cubicle reservation by deducting saldo from the student's monedero. No admin review required for payment.")
    public ResponseEntity<DataResponseDTO<ReservaAlumnoResponseDTO>> crearReservaConMonedero(
            @RequestParam("espacioId") Long espacioId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("horaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam("horaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam(value = "notas", required = false) String notas,
            @RequestParam("celular") String celular,
            @RequestParam("actividad") String actividad,
            @RequestParam("maestro") String maestro,
            @RequestParam(value = "productos", required = false) String productosJson,
            @RequestParam(value = "prestamos", required = false) String prestamosJson,
            @RequestParam(value = "pacienteId", required = false) Long pacienteId,
            @RequestParam(value = "pacienteNuevo", required = false) String pacienteNuevoJson,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {

        List<CrearReservaAlumnoDTO.ProductoLineaDTO> productos = Collections.emptyList();
        if (productosJson != null && !productosJson.isBlank()) {
            productos = objectMapper.readValue(productosJson,
                    new TypeReference<List<CrearReservaAlumnoDTO.ProductoLineaDTO>>() {});
        }
        List<CrearReservaAlumnoDTO.PrestamoLineaDTO> prestamos = Collections.emptyList();
        if (prestamosJson != null && !prestamosJson.isBlank()) {
            prestamos = objectMapper.readValue(prestamosJson,
                    new TypeReference<List<CrearReservaAlumnoDTO.PrestamoLineaDTO>>() {});
        }

        CrearReservaAlumnoDTO.PacienteNuevoDTO pacienteNuevo = null;
        if (pacienteNuevoJson != null && !pacienteNuevoJson.isBlank()) {
            pacienteNuevo = objectMapper.readValue(pacienteNuevoJson,
                    CrearReservaAlumnoDTO.PacienteNuevoDTO.class);
        }

        CrearReservaAlumnoDTO request = CrearReservaAlumnoDTO.builder()
                .espacioId(espacioId)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .notas(notas)
                .celular(celular)
                .actividad(actividad)
                .maestro(maestro)
                .productos(productos)
                .prestamos(prestamos)
                .pacienteId(pacienteId)
                .pacienteNuevo(pacienteNuevo)
                .build();

        ReservaAlumnoResponseDTO response = reservaAlumnoService.crearReservaConMonedero(
                request, userDetails.getId());

        return ResponseEntity.ok(DataResponseDTO.success(response, "Reserva confirmada con monedero exitosamente"));
    }

    @GetMapping("/mis-reservas")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Get my reservations", 
               description = "Get all reservations for the authenticated student")
    public ResponseEntity<DataResponseDTO<List<MisReservasDTO>>> obtenerMisReservas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<MisReservasDTO> reservas = reservaAlumnoService.obtenerMisReservas(
                userDetails.getId(), estado, fecha);

        return ResponseEntity.ok(DataResponseDTO.success(reservas, "Reservas obtenidas exitosamente"));
    }

    @GetMapping("/disponibilidad/{espacioId}/{fecha}")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Get availability", 
               description = "Get available time slots for a specific cubicle and date")
    public ResponseEntity<DataResponseDTO<DisponibilidadDTO>> obtenerDisponibilidad(
            @Parameter(description = "Space ID") @PathVariable Long espacioId,
            @Parameter(description = "Date") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        DisponibilidadDTO disponibilidad = reservaAlumnoService.obtenerDisponibilidad(espacioId, fecha);

        return ResponseEntity.ok(DataResponseDTO.success(disponibilidad, "Disponibilidad obtenida exitosamente"));
    }

    @GetMapping("/pacientes/buscar")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Buscar pacientes por nombre",
               description = "Devuelve pacientes activos cuyo nombre coincida con el término buscado. Usado para seleccionar paciente al crear reserva.")
    public ResponseEntity<DataResponseDTO<List<UsuarioDTO>>> buscarPacientes(
            @Parameter(description = "Término de búsqueda parcial") @RequestParam String nombre) {

        List<UsuarioDTO> pacientes = usuarioService.buscarPacientesPorNombre(nombre);
        return ResponseEntity.ok(DataResponseDTO.success(pacientes, "Pacientes encontrados"));
    }
}
