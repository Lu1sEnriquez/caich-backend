package com.api_caich.dto.pago;

import java.math.BigDecimal;

import com.api_caich.enums.MetodoPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagoRequestDTO {

    @NotNull(message = "El ticket ID es requerido")
    private Long ticketId;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es requerido")
    private MetodoPago metodoPago;

    private String comprobanteUrl;

    private String referencia;
}
