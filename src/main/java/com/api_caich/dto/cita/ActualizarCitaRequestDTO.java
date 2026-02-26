package com.api_caich.dto.cita;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ActualizarCitaRequestDTO {

    private Long espacioId;

    private String fecha; // ISO date-time string

    private Integer duracion; // minutos

    private Long pacienteId;

    private Long terapeutaId;

    private String materia;

    private String modalidad; // Presencial | Online

    private String estadoTicket;

    private String notas;

    private BigDecimal costoAdicional;

    private List<ProductoCitaDTO> productos;
}
