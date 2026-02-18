package com.api_caich.dto.reporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

/**
 * DTO para reportes de tickets con arquitectura modular
 */
@Data
@Builder
public class TicketReporteDTO {
    private String folio;
    private LocalDateTime fechaCreacion;
    
    // Información de la cita (si existe)
    private LocalDateTime fechaInicioCita;
    private String pacienteNombre;
    private String terapeutaNombre;
    
    // Información financiera
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    
    // Estados
    private EstadoTicket estadoTicket;
    private EstadoFinanciero estadoFinanciero;
    
    /**
     * Mapea una entidad Ticket a TicketReporteDTO
     */
    public static TicketReporteDTO fromEntity(com.api_caich.entity.Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        String pacienteNombre = null;
        String terapeutaNombre = null;
        LocalDateTime fechaInicioCita = null;
        
        // Extraer información de la cita si existe
        if (ticket.getCita() != null) {
            if (ticket.getCita().getPaciente() != null) {
                pacienteNombre = ticket.getCita().getPaciente().getNombreCompleto();
            }
            if (ticket.getCita().getTerapeuta() != null) {
                terapeutaNombre = ticket.getCita().getTerapeuta().getNombreCompleto();
            }
            fechaInicioCita = ticket.getCita().getFechaInicio();
        }

        return TicketReporteDTO.builder()
                .folio(ticket.getFolio())
                .fechaCreacion(ticket.getFechaCreacion())
                .fechaInicioCita(fechaInicioCita)
                .pacienteNombre(pacienteNombre)
                .terapeutaNombre(terapeutaNombre)
                .montoTotal(ticket.getMontoTotal())
                .montoPagado(ticket.getMontoPagado())
                .saldoPendiente(ticket.getSaldoPendiente())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoFinanciero(ticket.getEstadoFinanciero())
                .build();
    }
}
