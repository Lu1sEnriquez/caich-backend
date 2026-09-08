package com.api_caich.dto.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos que un alumno puede cambiar al reprogramar una reserva propia. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReprogramarReservaAlumnoDTO {

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime horaFin;
}
