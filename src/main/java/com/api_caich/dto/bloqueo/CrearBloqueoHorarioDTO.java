package com.api_caich.dto.bloqueo;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearBloqueoHorarioDTO {

    @NotNull(message = "La fecha inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha fin es requerida")
    private LocalDate fechaFin;

    @NotBlank(message = "El motivo es requerido")
    @Size(max = 300, message = "El motivo no puede exceder 300 caracteres")
    private String motivo;

    private Long espacioId; // Null = bloqueo global

    @NotNull(message = "El campo todoElDia es requerido")
    private Boolean todoElDia;
}
