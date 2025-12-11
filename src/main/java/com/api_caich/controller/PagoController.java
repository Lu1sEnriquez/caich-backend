package com.api_caich.controller;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.pago.RegistrarPagoDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/registrar-propio")
    // @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Register own payment", description = "Register payment for patient's own ticket")
    public ResponseEntity<DataResponseDTO<TicketDTO>> registrarPagoPropio(
            @Valid @RequestBody RegistrarPagoDTO registrarPagoDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TicketDTO ticket = pagoService.registrarPagoPropio(registrarPagoDTO, userDetails.getId());

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Pago registrado exitosamente"));
    }
}
