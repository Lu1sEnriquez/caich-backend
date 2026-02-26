package com.api_caich.dto.horario;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear o actualizar la configuración operativa de un espacio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionOperativaDTO {
    
    /**
     * ID del espacio. NULL para configuración global.
     */
    private Long espacioId;
    
    private String espacioNombre;

    @NotNull(message = "La hora de apertura es obligatoria")
    private LocalTime horaApertura;

    @NotNull(message = "La hora de cierre es obligatoria")
    private LocalTime horaCierre;

    @NotNull(message = "El intervalo en minutos es obligatorio")
    @Min(value = 15, message = "El intervalo mínimo es de 15 minutos")
    @Max(value = 240, message = "El intervalo máximo es de 240 minutos")
    private Integer intervaloMinutos;

    @NotNull(message = "La duración mínima es obligatoria")
    @Min(value = 15, message = "La duración mínima es de 15 minutos")
    private Integer duracionMinimaMinutos;

    @NotNull(message = "La duración máxima es obligatoria")
    @Min(value = 30, message = "La duración máxima mínima es de 30 minutos")
    private Integer duracionMaximaMinutos;

    /**
     * Días operativos: 1=Lunes, 2=Martes, ..., 7=Domingo
     * Si es null o vacío, todos los días son operativos.
     */
    private List<Integer> diasOperativos;

    private Boolean estaActivo;
}
