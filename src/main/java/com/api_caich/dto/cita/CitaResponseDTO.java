package com.api_caich.dto.cita;

import java.math.BigDecimal;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para citas, optimizado para la vista de calendario.
 * Mapea los datos del ticket+cita+espacio a un formato plano.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponseDTO {

    private Long ticketId;
    private Long espacioId;
    private String espacioNombre;
    private String fecha; // ISO date-time
    private Integer duracion; // minutos
    private Long pacienteId;
    private String pacienteNombre;
    private Long terapeutaId;
    private String terapeutaNombre;
    private String materia;
    private String modalidad;
    private EstadoTicket estadoTicket;
    private EstadoFinanciero estadoPago;
    private String notas;
    private BigDecimal montoPagado;
    private BigDecimal costoEspacio;
    private BigDecimal costoAdicional;
    private BigDecimal costoTotal;
    private String fechaCreacion;
}
