package com.api_caich.dto.horario;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear o actualizar un HorarioBase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioBaseDTO {

    @JsonProperty("horarioBaseId")
    private Long horarioBaseId;

    @JsonProperty("espacioId")
    private Long espacioId;

    @NotNull(message = "La hora de apertura es requerida")
    @JsonProperty("horaApertura")
    private LocalTime horaApertura;

    @NotNull(message = "La hora de cierre es requerida")
    @JsonProperty("horaCierre")
    private LocalTime horaCierre;

    @JsonProperty("diasOperativos")
    @Pattern(regexp = "^([1-7],)*[1-7]$|^$", message = "Formato inválido. Usa números del 1-7 separados por comas (ej: 1,2,3,4,5)")
    private String diasOperativos; // "1,2,3,4,5" = Lunes a Viernes

    @NotNull(message = "El intervalo en minutos es requerido")
    @Min(value = 15, message = "El intervalo mínimo es 15 minutos")
    @JsonProperty("intervaloMinutos")
    private Integer intervaloMinutos;

    @NotNull(message = "La duración mínima es requerida")
    @Min(value = 15, message = "La duración mínima es de 15 minutos")
    @JsonProperty("duracionMinimaMinutos")
    private Integer duracionMinimaMinutos;

    @NotNull(message = "La duración máxima es requerida")
    @Min(value = 30, message = "La duración máxima debe ser al menos 30 minutos")
    @JsonProperty("duracionMaximaMinutos")
    private Integer duracionMaximaMinutos;

    @Builder.Default
    @JsonProperty("estaActivo")
    private Boolean estaActivo = true;
}
