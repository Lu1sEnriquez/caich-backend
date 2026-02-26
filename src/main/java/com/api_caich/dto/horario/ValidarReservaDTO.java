package com.api_caich.dto.horario;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para validar una reserva de horario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarReservaDTO {
    
    @NotNull(message = "El espacio es obligatorio")
    private Long espacioId;
    
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;
    
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}
