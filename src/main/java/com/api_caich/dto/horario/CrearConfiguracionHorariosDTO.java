package com.api_caich.dto.horario;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearConfiguracionHorariosDTO {
    @NotNull(message = "El espacio es obligatorio")
    private Long espacioId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "Las horas deshabilitadas son obligatorias")
    private List<String> horasDeshabilitadas;
}