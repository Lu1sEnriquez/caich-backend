package com.api_caich.dto.monedero;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearRecargaMonederoRequest {

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "10.00", message = "El monto mínimo de recarga es $10.00")
    private BigDecimal monto;

    private String referenciaBancaria;

    private String notasAlumno;
}
