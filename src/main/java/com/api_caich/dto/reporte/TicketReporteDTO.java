package com.api_caich.dto.reporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketReporteDTO {
    private String folio;
    private LocalDateTime fecha;
    private String pacienteNombre;
    private String terapeutaNombre;
    private String materia;
    private BigDecimal costoTotal;
    private String estadoTicket;
    private String estadoPago;
}