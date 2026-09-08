package com.api_caich.dto.monedero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SolicitudDevolucionDTO {
    private Long solicitudDevolucionId;
    private Long ticketId;
    private String ticketFolio;
    private Long alumnoId;
    private String alumnoNombre;
    private BigDecimal montoDevolucion;
    private BigDecimal montoAprobado;
    private String estado;              // EstadoDevolucionMonedero.name()
    private String motivoCancelacion;
    private String motivoAprobacion;
    private String motivoRechazo;
    private BigDecimal ticketMontoTotal;
    private BigDecimal ticketMontoPagado;
    private String espacioNombre;
    private LocalDateTime horaInicioReserva;
    private LocalDateTime horaFinReserva;
    private String procesadoPorNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaProcesamiento;
}
