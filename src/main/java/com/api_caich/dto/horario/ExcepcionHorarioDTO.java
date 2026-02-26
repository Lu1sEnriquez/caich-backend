package com.api_caich.dto.horario;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear o actualizar una ExcepcionHorario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcepcionHorarioDTO {

    @JsonProperty("excepcionHorarioId")
    private Long excepcionHorarioId;

    @JsonProperty("espacioId")
    private Long espacioId;

    @NotNull(message = "La fecha es requerida")
    @JsonProperty("fecha")
    private LocalDate fecha;

    @NotNull(message = "Especifica si es todo el día")
    @Builder.Default
    @JsonProperty("todoElDia")
    private Boolean todoElDia = false;

    @JsonProperty("horaInicioBloqueo")
    private LocalTime horaInicioBloqueo;

    @JsonProperty("horaFinBloqueo")
    private LocalTime horaFinBloqueo;

    @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
    @JsonProperty("motivo")
    private String motivo;

    @Builder.Default
    @JsonProperty("estaActivo")
    private Boolean estaActivo = true;
}
