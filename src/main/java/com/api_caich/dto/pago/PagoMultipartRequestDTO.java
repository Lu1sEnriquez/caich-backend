package com.api_caich.dto.pago;

import java.math.BigDecimal;

import com.api_caich.enums.MetodoPago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * DTO para la parte JSON de un request multipart de pago.
 * Usado junto con MultipartFile en endpoints que aceptan comprobantes.
 */
@Data
public class PagoMultipartRequestDTO {

    @NotNull(message = "El ticketId es obligatorio")
    private Long ticketId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    private String referencia;

    private String notas;
}
