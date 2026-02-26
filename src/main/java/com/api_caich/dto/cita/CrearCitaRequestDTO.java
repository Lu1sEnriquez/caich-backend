package com.api_caich.dto.cita;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearCitaRequestDTO {

    // Opcional: solo requerido para modalidad Presencial
    private Long espacioId;

    @NotNull(message = "La fecha es requerida")
    private String fecha; // ISO date-time string e.g. "2026-02-20T10:00:00"

    @NotNull(message = "La duración es requerida")
    private Integer duracion; // minutos

    @NotNull(message = "El paciente es requerido")
    private Long pacienteId;

    @NotNull(message = "El terapeuta es requerido")
    private Long terapeutaId;

    private String materia;

    private String modalidad = "Presencial"; // Presencial | Online

    private String notas;

    private BigDecimal montoPagado;

    private BigDecimal costoAdicional;

    private List<ProductoCitaDTO> productos;

    private Long cuentaDestinoId;
}
