package com.api_caich.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

import lombok.Builder;
import lombok.Data;

/**
 * DTO simplificado para listados y filtros de tickets
 */
@Data
@Builder
public class TicketFiltroDTO {

    private Long ticketId;
    private String folio;

    // Creador del ticket
    private String creadoPorNombre;
    private String creadoPorEmail;

    // Estados
    private EstadoTicket estadoTicket;
    private EstadoFinanciero estadoFinanciero;

    // Información de la cita (si existe)
    private String pacienteNombre;
    private String pacienteEmail;
    private String terapeutaNombre;
    private String terapeutaEmail;
    private LocalDateTime fechaInicioCita;
    private String modalidad; // Presencial u Online

    // Información del cubículo/espacio (si existe)
    private Long espacioId;
    private String espacioNombre;

    // Información financiera
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;

    // Auditoría
    private LocalDateTime fechaCreacion;

    /**
     * Mapea una entidad Ticket a TicketFiltroDTO
     */
    public static TicketFiltroDTO fromEntity(com.api_caich.entity.Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        String pacienteNombre = null;
        String pacienteEmail = null;
        String terapeutaNombre = null;
        String terapeutaEmail = null;
        LocalDateTime fechaInicioCita = null;
        Long espacioId = null;
        String espacioNombre = null;
        String modalidad = "Presencial"; // Default

        // Extraer información de la cita si existe
        if (ticket.getCita() != null) {
            if (ticket.getCita().getPaciente() != null) {
                pacienteNombre = ticket.getCita().getPaciente().getNombreCompleto();
                pacienteEmail = ticket.getCita().getPaciente().getEmail();
            }
            if (ticket.getCita().getTerapeuta() != null) {
                terapeutaNombre = ticket.getCita().getTerapeuta().getNombreCompleto();
                terapeutaEmail = ticket.getCita().getTerapeuta().getEmail();
            }
            fechaInicioCita = ticket.getCita().getFechaInicio();
            modalidad = ticket.getCita().getModalidad() != null ? ticket.getCita().getModalidad() : "Presencial";
        }
        
        // Obtener espacio de TicketEspacio (primera reserva si existe)
        if (ticket.getEspacios() != null && !ticket.getEspacios().isEmpty()) {
            var primeraReserva = ticket.getEspacios().get(0);
            if (primeraReserva.getEspacio() != null) {
                espacioId = primeraReserva.getEspacio().getEspacioId();
                espacioNombre = primeraReserva.getEspacio().getNombre();
            }
        }

        return TicketFiltroDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(ticket.getFolio())
                .creadoPorNombre(ticket.getCreadoPor().getNombreCompleto())
                .creadoPorEmail(ticket.getCreadoPor().getEmail())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoFinanciero(ticket.getEstadoFinanciero())
                .pacienteNombre(pacienteNombre)
                .pacienteEmail(pacienteEmail)
                .terapeutaNombre(terapeutaNombre)
                .terapeutaEmail(terapeutaEmail)
                .fechaInicioCita(fechaInicioCita)
                .modalidad(modalidad)
                .espacioId(espacioId)
                .espacioNombre(espacioNombre)
                .montoTotal(ticket.getMontoTotal())
                .montoPagado(ticket.getMontoPagado())
                .saldoPendiente(ticket.getSaldoPendiente())
                .fechaCreacion(ticket.getFechaCreacion())
                .build();
    }
}
