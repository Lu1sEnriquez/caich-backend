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
    private String estado;              // EstadoDevolucionMonedero.name()
    private String motivoRechazo;
    private String procesadoPorNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaProcesamiento;
}
