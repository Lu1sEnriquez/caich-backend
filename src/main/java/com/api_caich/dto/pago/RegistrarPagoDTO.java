package com.api_caich.dto.pago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegistrarPagoDTO {

    @NotNull(message = "El ticket ID es requerido")
    private Long ticketId;

    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal montoPagado;

    @NotNull(message = "La cuenta destino es requerida")
    private Long cuentaDestinoId;

    private String conceptoTransferencia;
    private String celular;
}
