package com.api_caich.dto.bloqueo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueoHorarioDTO {
    private Long bloqueoId;
    private Long espacioId;
    private String espacioNombre;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String motivo;
    private Boolean todoElDia;
    private String creadoPor;
    private LocalDateTime fechaCreacion;
}
