package com.api_caich.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prestamos")
@RequiredArgsConstructor
@Tag(name = "Prestamos", description = "APIs for managing product loans")
@SecurityRequirement(name = "bearerAuth")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get loans", description = "Get paginated loans with optional status filter")
    public ResponseEntity<DataResponseDTO<Page<PrestamoDTO>>> obtenerPrestamos(
            @RequestParam(required = false) String estado,
            Pageable pageable) {

        Page<PrestamoDTO> prestamos = prestamoService.obtenerPrestamos(estado, pageable);
        return ResponseEntity.ok(DataResponseDTO.success(prestamos, "Prestamos obtenidos"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Create loan", description = "Create a new product loan")
    public ResponseEntity<DataResponseDTO<PrestamoDTO>> crearPrestamo(
            @Valid @RequestBody CrearPrestamoDTO request,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        PrestamoDTO prestamo = prestamoService.crearPrestamo(request, authentication.getId());
        return ResponseEntity.ok(DataResponseDTO.success(prestamo, "Prestamo creado"));
    }

    @PutMapping("/{prestamoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update loan", description = "Update loan data")
    public ResponseEntity<DataResponseDTO<PrestamoDTO>> actualizarPrestamo(
            @PathVariable Long prestamoId,
            @Valid @RequestBody CrearPrestamoDTO request,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        PrestamoDTO prestamo = prestamoService.actualizarPrestamo(prestamoId, request, authentication.getId());
        return ResponseEntity.ok(DataResponseDTO.success(prestamo, "Prestamo actualizado"));
    }

    @PutMapping("/{prestamoId}/devolver")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Return loan", description = "Return loan with optional observations")
    public ResponseEntity<BaseResponseDTO> devolverPrestamo(
            @PathVariable Long prestamoId,
            @RequestParam(required = false) String observaciones,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        prestamoService.devolverPrestamo(prestamoId, observaciones, authentication.getId());
        return ResponseEntity.ok(BaseResponseDTO.success("Prestamo devuelto"));
    }
}