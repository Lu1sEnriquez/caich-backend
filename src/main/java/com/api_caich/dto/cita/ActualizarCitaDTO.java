package com.api_caich.dto.cita;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarCitaDTO {
    private Long espacioId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fecha;

    private Integer duracion;
    private Long pacienteId;
    private Long terapeutaId;
    private String materia;
    private String modalidad;
    private String estadoTicket;
    private String notas;
}