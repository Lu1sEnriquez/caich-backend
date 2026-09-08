package com.api_caich.dto.monedero;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobarDevolucionRequest {
    @NotNull(message = "El monto a devolver es requerido")
    @DecimalMin(value = "0.01", message = "El monto a devolver debe ser mayor a cero")
    private BigDecimal montoAprobado;

    /** Nota interna opcional para justificar una devolución parcial. */
    private String motivoAprobacion;
}
