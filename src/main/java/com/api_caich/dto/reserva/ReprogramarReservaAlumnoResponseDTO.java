package com.api_caich.dto.reserva;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Resultado económico de una reprogramación de reserva pagada con monedero. */
@Getter
@Builder
@AllArgsConstructor
public class ReprogramarReservaAlumnoResponseDTO {
    private final BigDecimal nuevoCosto;
    /** Positivo = cargo adicional; negativo = saldo acreditado al monedero. */
    private final BigDecimal diferencia;
}
