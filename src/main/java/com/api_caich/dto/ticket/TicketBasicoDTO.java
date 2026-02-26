package com.api_caich.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.entity.Ticket;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas básicas de Tickets (listados, búsquedas)
 * Solo incluye datos esenciales sin información detallada de citas, espacios, productos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketBasicoDTO {
    private Long ticketId;
    private String folio;
    private LocalDateTime fechaCreacion;
    private EstadoTicket estadoTicket;
    private EstadoFinanciero estadoFinanciero;
    private BigDecimal total;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    
    // Info mínima de creador
    private Long creadoPorId;
    private String creadoPorNombre;
    
    // Info mínima de paciente si tiene cita
    private Long pacienteId;
    private String pacienteNombre;
    
    // Info mínima de terapeuta si tiene cita
    private Long terapeutaId;
    private String terapeutaNombre;
    
    // Fecha de cita si existe
    private LocalDateTime citaFechaInicio;
    private LocalDateTime citaFechaFin;
    
    // Indicadores de tipo de ticket
    private boolean tieneCita;
    private boolean tieneEspacios;
    private boolean tieneVentas;
    private boolean tienePrestamos;

    public static TicketBasicoDTO fromEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        TicketBasicoDTOBuilder builder = TicketBasicoDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(ticket.getFolio())
                .fechaCreacion(ticket.getFechaCreacion())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoFinanciero(ticket.getEstadoFinanciero())
                .total(ticket.getMontoTotal())
                .montoPagado(ticket.getMontoPagado())
                .saldoPendiente(ticket.getSaldoPendiente())
                .creadoPorId(ticket.getCreadoPor().getUsuarioId())
                .creadoPorNombre(ticket.getCreadoPor().getNombreCompleto())
                .tieneCita(ticket.getCita() != null)
                .tieneEspacios(ticket.getEspacios() != null && !ticket.getEspacios().isEmpty())
                .tieneVentas(ticket.getVentas() != null && !ticket.getVentas().isEmpty())
                .tienePrestamos(ticket.getPrestamos() != null && !ticket.getPrestamos().isEmpty());

        // Información de cita si existe
        if (ticket.getCita() != null) {
            builder.citaFechaInicio(ticket.getCita().getFechaInicio())
                   .citaFechaFin(ticket.getCita().getFechaFin());
            
            if (ticket.getCita().getPaciente() != null) {
                builder.pacienteId(ticket.getCita().getPaciente().getUsuarioId())
                       .pacienteNombre(ticket.getCita().getPaciente().getNombreCompleto());
            }
            
            if (ticket.getCita().getTerapeuta() != null) {
                builder.terapeutaId(ticket.getCita().getTerapeuta().getUsuarioId())
                       .terapeutaNombre(ticket.getCita().getTerapeuta().getNombreCompleto());
            }
        }

        return builder.build();
    }
}
