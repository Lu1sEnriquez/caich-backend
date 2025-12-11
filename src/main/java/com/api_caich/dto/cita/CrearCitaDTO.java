package com.api_caich.dto.cita;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearCitaDTO {
    @NotNull(message = "El espacio es obligatorio")
    private Long espacioId;

    @NotNull(message = "La fecha y hora son obligatorias")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fecha;

    @NotNull(message = "La duración es obligatoria")
    private Integer duracion; // en minutos

    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El terapeuta es obligatorio")
    private Long terapeutaId;

    @NotBlank(message = "La materia/motivo es obligatoria")
    private String materia;

    private String celular;

    @NotBlank(message = "La modalidad es obligatoria")
    private String modalidad; // "Presencial" | "Online"

    private String notas;

    // Campos de pago (pueden tener valores por defecto para citas)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @NotNull(message = "La cuenta destino es obligatoria")
    private Long cuentaDestinoId;
}
