package com.api_caich.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.prestamo.CrearPrestamoDTO;
import com.api_caich.dto.prestamo.PrestamoDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.PrestamoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prestamos")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "APIs for managing material loans")
@SecurityRequirement(name = "bearerAuth")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Create new loan", description = "Create a material loan with stock validation")
    public ResponseEntity<DataResponseDTO<PrestamoDTO>> crearPrestamo(
            @Valid @RequestBody CrearPrestamoDTO crearPrestamoDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PrestamoDTO prestamo;

        // Determinar quién crea el préstamo
        if (userDetails.getRol().equals("Alumno")) {
            prestamo = prestamoService.crearPrestamoAlumno(crearPrestamoDTO, userDetails.getId());
        } else {
            prestamo = prestamoService.crearPrestamo(crearPrestamoDTO, userDetails.getId());
        }

        return ResponseEntity.ok(DataResponseDTO.success(prestamo, "Préstamo creado exitosamente"));
    }

    @PutMapping("/{prestamoId}/devolver")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Return loan", description = "Return a loan and update stock")
    public ResponseEntity<DataResponseDTO<PrestamoDTO>> devolverPrestamo(
            @Parameter(description = "Loan ID") @PathVariable Long prestamoId,
            @RequestParam(required = false) String observaciones,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PrestamoDTO prestamo;

        // Determinar quién devuelve el préstamo
        if (userDetails.getRol().equals("Alumno")) {
            prestamo = prestamoService.devolverPrestamoAlumno(prestamoId, userDetails.getId(), observaciones);
        } else {
            prestamo = prestamoService.devolverPrestamo(prestamoId, observaciones, userDetails.getId());
        }

        return ResponseEntity.ok(DataResponseDTO.success(prestamo, "Préstamo devuelto exitosamente"));
    }

    @PostMapping("/{prestamoId}/marcar-perdido")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark loan as lost", description = "Mark loan as lost with mandatory observations")
    public ResponseEntity<BaseResponseDTO> marcarComoPerdido(
            @PathVariable Long prestamoId,
            @RequestParam String observaciones,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        prestamoService.marcarComoPerdido(prestamoId, observaciones, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Loan marked as lost"));
    }

    @GetMapping("/alumno/{alumnoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get active loans by student", description = "Get active and overdue loans for a student")
    public ResponseEntity<DataResponseDTO<List<PrestamoDTO>>> obtenerPrestamosActivosPorAlumno(
            @PathVariable Long alumnoId) {

        List<PrestamoDTO> prestamos = prestamoService.obtenerPrestamosActivosPorAlumno(alumnoId);
        return ResponseEntity.ok(DataResponseDTO.success(prestamos, "Active loans retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get loans by status", description = "Get loans filtered by status")
    public ResponseEntity<DataResponseDTO<List<PrestamoDTO>>> obtenerPrestamosPorEstado(
            @RequestParam(required = false) String estado) {

        List<PrestamoDTO> prestamos = prestamoService.obtenerPrestamosPorEstado(estado);
        return ResponseEntity.ok(DataResponseDTO.success(prestamos, "Loans retrieved successfully"));
    }

    @GetMapping("/mis-prestamos")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Get my loans", description = "Get paginated list of student's own loans")
    public ResponseEntity<DataResponseDTO<Page<PrestamoDTO>>> obtenerMisPrestamos(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fechaPrestamo") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PrestamoDTO> prestamos = prestamoService.obtenerMisPrestamos(userDetails.getId(), pageable);

        return ResponseEntity.ok(DataResponseDTO.success(prestamos, "Mis préstamos recuperados exitosamente"));
    }

    @DeleteMapping("/{prestamoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Delete loan", description = "Delete loan (only active loans can be deleted)")
    public ResponseEntity<BaseResponseDTO> eliminarPrestamo(
            @Parameter(description = "Loan ID") @PathVariable Long prestamoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        prestamoService.eliminarPrestamo(prestamoId, userDetails.getId());

        return ResponseEntity.ok(BaseResponseDTO.success("Préstamo eliminado exitosamente"));
    }
}
