package com.api_caich.dto.cita;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaFiltroDTO {
    private Long citaId;
    private String espacioNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String pacienteNombre;
    private String terapeutaNombre;
    private String modalidad;
    private String estado;
    private LocalDateTime fechaCreacion;
}
