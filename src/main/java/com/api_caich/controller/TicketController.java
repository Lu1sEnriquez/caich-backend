package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.ticket.CrearTicketDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketFiltroDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.ComprobanteService;
import com.api_caich.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "APIs for managing tickets and appointments")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final ComprobanteService comprobanteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Create new ticket", description = "Create a complete ticket with products and notifications")
    public ResponseEntity<DataResponseDTO<TicketDTO>> crearTicket(
            @Valid @RequestBody CrearTicketDTO crearTicketDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.crearTicketCompleto(crearTicketDTO, usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket created successfully"));
    }

    @PutMapping("/{ticketId}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update ticket status", description = "Update ticket and payment status with validations")
    public ResponseEntity<DataResponseDTO<TicketDTO>> actualizarEstado(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @RequestParam(required = false) String estadoTicket,
            @RequestParam(required = false) String estadoPago,
            @RequestParam(required = false) String notas,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.actualizarEstado(ticketId, estadoTicket, estadoPago, notas, usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket status updated successfully"));
    }

    @PostMapping("/{ticketId}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Cancel ticket", description = "Cancel ticket and return stock if applicable")
    public ResponseEntity<BaseResponseDTO> cancelarTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @RequestParam String motivo,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        ticketService.cancelarTicket(ticketId, motivo, usuarioId);
        
        return ResponseEntity.ok(BaseResponseDTO.success("Ticket cancelled successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Get tickets with filters", description = "Get paginated and filtered tickets based on user role")
    public ResponseEntity<DataResponseDTO<Page<TicketFiltroDTO>>> obtenerTickets(
            @Parameter(description = "Patient ID") @RequestParam(required = false) Long pacienteId,
            @Parameter(description = "Therapist ID") @RequestParam(required = false) Long terapeutaId,
            @Parameter(description = "Ticket status") @RequestParam(required = false) String estadoTicket,
            @Parameter(description = "Payment status") @RequestParam(required = false) String estadoPago,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fecha") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Convertir fechas a LocalDateTime
        LocalDateTime fechaInicioDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fechaFinDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        // Configurar paginación
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TicketFiltroDTO> tickets = ticketService.obtenerTicketsConFiltros(
                userDetails.getId(),
                userDetails.getRol(),
                pacienteId,
                terapeutaId,
                estadoTicket,
                estadoPago,
                fechaInicioDateTime,
                fechaFinDateTime,
                pageable);

        return ResponseEntity.ok(DataResponseDTO.success(tickets, "Tickets retrieved successfully"));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Get ticket by ID", description = "Get ticket details with access control based on user role")
    public ResponseEntity<DataResponseDTO<TicketDTO>> obtenerTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TicketDTO ticket = ticketService.obtenerTicketConSeguridad(
                ticketId, userDetails.getId(), userDetails.getRol());

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket retrieved successfully"));
    }

    @PostMapping("/{ticketId}/comprobante")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Upload payment receipt", description = "Upload payment receipt for a ticket")
    public ResponseEntity<BaseResponseDTO> subirComprobante(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Parameter(description = "Payment receipt file") @RequestParam("comprobante") MultipartFile archivo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        comprobanteService.subirComprobantePago(ticketId, archivo, userDetails.getId());

        return ResponseEntity.ok(BaseResponseDTO.success("Comprobante subido exitosamente"));
    }

    @GetMapping("/mis-tickets")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Get my tickets", description = "Get paginated list of patient's own tickets")
    public ResponseEntity<DataResponseDTO<Page<TicketDTO>>> obtenerMisTickets(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fecha") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TicketDTO> tickets = ticketService.obtenerMisTickets(userDetails.getId(), pageable);

        return ResponseEntity.ok(DataResponseDTO.success(tickets, "Mis tickets recuperados exitosamente"));
    }
}