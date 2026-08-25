package com.api_caich.dto.monedero;

import java.math.BigDecimal;


import com.api_caich.enums.MetodoPago;
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

    /** Método de pago: TRANSFERENCIA, TESORERIA, etc. */
    private MetodoPago metodoPago = MetodoPago.TRANSFERENCIA;

    /** Concepto de pago (motivo, referencia para ticket). */
    private String concepto;
}
