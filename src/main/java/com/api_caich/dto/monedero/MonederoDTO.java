package com.api_caich.dto.monedero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MonederoDTO {
    private Long monederoId;
    private Long alumnoId;
    private String alumnoNombre;
    private String alumnoEmail;
    private String alumnoFolio;
    private BigDecimal saldo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;
}
