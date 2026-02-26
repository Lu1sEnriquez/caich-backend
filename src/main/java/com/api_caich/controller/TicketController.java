package com.api_caich.controller;

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
import com.api_caich.dto.ticket.TicketBasicoDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketEspacioDTO;
import com.api_caich.dto.ticket.TicketProductosUpdateDTO;
import com.api_caich.dto.ticket.TicketRequestDTO;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "APIs for managing tickets with modular architecture")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Create new ticket", description = "Create a complete ticket with products, appointments, spaces and loans")
    public ResponseEntity<DataResponseDTO<TicketDTO>> crearTicket(
            @Valid @RequestBody TicketRequestDTO ticketRequest,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        TicketDTO ticket = ticketService.crearTicket(ticketRequest);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket created successfully"));
    }

    @PutMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update complete ticket", description = "Update entire ticket including cita, espacios, ventas, and prestamos")
    public ResponseEntity<DataResponseDTO<TicketDTO>> actualizarTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketRequestDTO ticketRequest,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.actualizarTicket(ticketId, ticketRequest, usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket updated successfully"));
    }

    @PutMapping("/{ticketId}/espacios/{reservaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update space reservation", description = "Update a cubicle reservation for a ticket")
    public ResponseEntity<DataResponseDTO<TicketDTO>> actualizarReservaEspacio(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Parameter(description = "Space reservation ID") @PathVariable Long reservaId,
            @Valid @RequestBody TicketEspacioDTO request,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.actualizarReservaEspacio(ticketId, reservaId, request, usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Space reservation updated successfully"));
    }

    @PutMapping("/{ticketId}/productos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update ticket products", description = "Replace products (ventas/prestamos) for a ticket")
    public ResponseEntity<DataResponseDTO<TicketDTO>> actualizarProductos(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketProductosUpdateDTO request,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.actualizarProductos(ticketId, request, usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket products updated successfully"));
    }

    @PutMapping("/{ticketId}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Update ticket status", description = "Update ticket status with validations")
    public ResponseEntity<DataResponseDTO<TicketDTO>> actualizarEstado(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @RequestParam EstadoTicket estadoTicket,
            @RequestParam(required = false) String notas,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        TicketDTO ticket = ticketService.actualizarEstado(ticketId, estadoTicket, notas, usuarioId);

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
    @Operation(summary = "Get tickets with filters (basic info)", description = "Get paginated and filtered tickets with basic information based on user role")
    public ResponseEntity<DataResponseDTO<Page<TicketBasicoDTO>>> obtenerTicketsBasico(
            @Parameter(description = "Patient ID") @RequestParam(required = false) Long pacienteId,
            @Parameter(description = "Therapist ID") @RequestParam(required = false) Long terapeutaId,
            @Parameter(description = "Ticket status") @RequestParam(required = false) EstadoTicket estadoTicket,
            @Parameter(description = "Financial status") @RequestParam(required = false) EstadoFinanciero estadoFinanciero,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Start datetime (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeStart,
            @Parameter(description = "End datetime (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeEnd,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Usar timeStart/timeEnd si se proporcionan, sino usar fechaInicio/fechaFin
        LocalDateTime fechaInicioDateTime = timeStart != null ? timeStart 
                : (fechaInicio != null ? fechaInicio.atStartOfDay() : null);
        LocalDateTime fechaFinDateTime = timeEnd != null ? timeEnd 
                : (fechaFin != null ? fechaFin.atTime(23, 59, 59) : null);

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TicketBasicoDTO> tickets = ticketService.obtenerTicketsBasicos(
                userDetails.getId(),
                userDetails.getRol(),
                pacienteId,
                terapeutaId,
                estadoTicket,
                estadoFinanciero,
                fechaInicioDateTime,
                fechaFinDateTime,
                pageable);

        return ResponseEntity.ok(DataResponseDTO.success(tickets, "Tickets retrieved successfully"));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Get ticket by ID (detailed)", description = "Get complete ticket details with all related data, with access control based on user role")
    public ResponseEntity<DataResponseDTO<TicketDTO>> obtenerTicketDetallado(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TicketDTO ticket = ticketService.obtenerTicketConSeguridad(
                ticketId, userDetails.getId(), userDetails.getRol());

        return ResponseEntity.ok(DataResponseDTO.success(ticket, "Ticket retrieved successfully"));
    }

    @GetMapping("/mis-tickets")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Get my tickets", description = "Get paginated list of patient's own tickets")
    public ResponseEntity<DataResponseDTO<Page<TicketDTO>>> obtenerMisTickets(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TicketDTO> tickets = ticketService.obtenerMisTickets(userDetails.getId(), pageable);

        return ResponseEntity.ok(DataResponseDTO.success(tickets, "My tickets retrieved successfully"));
    }
}
