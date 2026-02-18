package com.api_caich.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCitaDTO {

    private Long citaId;

    private Long pacienteId;

    private String pacienteNombre;

    private Long terapeutaId;

    private String terapeutaNombre;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDateTime fechaFin;

    @NotNull(message = "El precio cobrado es requerido")
    @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
    private BigDecimal precioCobrado;

    private String notasTerapia;
}
