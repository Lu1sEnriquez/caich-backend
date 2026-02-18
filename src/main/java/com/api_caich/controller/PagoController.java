package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.pago.PagoDTO;
import com.api_caich.dto.pago.PagoRequestDTO;
import com.api_caich.entity.Pago;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.PagoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "APIs for payment management")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Register payment", description = "Register a new payment for a ticket")
    public ResponseEntity<DataResponseDTO<PagoDTO>> registrarPago(
            @Valid @RequestBody PagoRequestDTO pagoRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.registrarPago(pagoRequest, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment registered successfully"));
    }

    @PutMapping("/{pagoId}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve payment", description = "Approve and verify a payment")
    public ResponseEntity<DataResponseDTO<PagoDTO>> aprobarPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.aprobarPago(pagoId, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment approved successfully"));
    }

    @PutMapping("/{pagoId}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject payment", description = "Reject a payment with reason")
    public ResponseEntity<DataResponseDTO<PagoDTO>> rechazarPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId,
            @RequestParam String motivo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.rechazarPago(pagoId, motivo, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment rejected successfully"));
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Get payments for ticket", description = "Get all payments associated with a specific ticket")
    public ResponseEntity<DataResponseDTO<List<PagoDTO>>> obtenerPagosDeTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {

        List<Pago> pagos = pagoService.obtenerPagosDeTicket(ticketId);
        List<PagoDTO> pagoDTOs = pagos.stream()
                .map(PagoDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTOs, "Payments retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments", description = "Get paginated list of all payments (admin only)")
    public ResponseEntity<DataResponseDTO<Page<PagoDTO>>> obtenerTodosPagos(
            @Parameter(description = "Payment status") @RequestParam(required = false) EstadoPagoTransaccion estado,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fechaPago") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Pago> pagos = estado != null 
            ? pagoService.obtenerPagosConEstado(estado, pageable)
            : pagoService.obtenerTodosPagos(pageable);

        Page<PagoDTO> pagoDTOs = pagos.map(PagoDTO::fromEntity);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTOs, "Payments retrieved successfully"));
    }

    @GetMapping("/{pagoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Get payment by ID", description = "Get details of a specific payment")
    public ResponseEntity<DataResponseDTO<PagoDTO>> obtenerPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId) {

        Pago pago = pagoService.obtenerPago(pagoId);
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment retrieved successfully"));
    }
}
