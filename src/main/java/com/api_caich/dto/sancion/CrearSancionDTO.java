package com.api_caich.dto.sancion;

import java.time.LocalDate;

import com.api_caich.enums.TipoSancion;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSancionDTO {

    @NotNull(message = "El alumno ID es requerido")
    private Long alumnoId;

    @NotNull(message = "El tipo de sanción es requerido")
    private TipoSancion tipoSancion;

    @NotNull(message = "El motivo es requerido")
    private String motivo;

    private String descripcion;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private Long ticketRelacionadoId;
}
