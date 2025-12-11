package com.api_caich.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketFiltroDTO {
    private Long ticketId;
    private String folio;
    private LocalDateTime fecha;
    private String pacienteNombre;
    private String pacienteEmail;
    private String terapeutaNombre;
    private String terapeutaEmail;
    private String espacioNombre;
    private String materia;
    private BigDecimal costoTotal;
    private String estadoTicket;
    private String estadoPago;
    private LocalDateTime fechaCreacion;
}