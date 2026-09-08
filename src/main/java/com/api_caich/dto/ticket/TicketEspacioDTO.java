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
public class TicketEspacioDTO {

    private Long espacioReservaId;

    @NotNull(message = "El espacio ID es requerido")
    private Long espacioId;

    private String espacioNombre;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalDateTime horaInicioReserva;

    @NotNull(message = "La hora de fin es requerida")
    private LocalDateTime horaFinReserva;

    @NotNull(message = "El costo cobrado es requerido")
    @DecimalMin(value = "0.0", message = "El costo debe ser mayor o igual a 0")
    private BigDecimal costoCobrado;

    private String celular;
    private String actividad;
    private String maestro;
}
