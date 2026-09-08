package com.api_caich.dto.reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisReservasDTO {

    private Long ticketId;
    private Long espacioReservaId;
    private Long espacioId;
    private String folio;
    private String espacioNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal costo;
    private String estadoTicket;
    private String estadoPago;
    private String urlComprobante;
    private String estadoVerificacionComprobante;
    private String motivoRechazo;
    private String celular;
    private String actividad;
    private String maestro;
    private String idAlumno;
}
