package com.api_caich.dto.monedero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RecargaMonederoDTO {
    private Long recargaMonederoId;
    private Long alumnoId;
    private String alumnoNombre;
    private String alumnoEmail;
    private String alumnoFolio;
    private BigDecimal monto;
    private String comprobanteUrl;
    private String referenciaBancaria;
    private String notasAlumno;
    private String estado;          // EstadoRecargaMonedero.name()
    private String motivoRechazo;
    private String verificadoPorNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaVerificacion;
    private Integer numeroIntento;
}
