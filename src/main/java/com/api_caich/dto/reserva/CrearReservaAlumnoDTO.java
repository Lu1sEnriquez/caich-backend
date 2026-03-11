package com.api_caich.dto.reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearReservaAlumnoDTO {

    @NotNull(message = "El espacio ID es requerido")
    private Long espacioId;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime horaFin;

    private String notas;

    @NotBlank(message = "El celular es requerido")
    private String celular;

    @NotBlank(message = "La actividad es requerida")
    private String actividad;

    @NotBlank(message = "El maestro es requerido")
    private String maestro;

    private List<ProductoLineaDTO> productos;
    private List<PrestamoLineaDTO> prestamos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoLineaDTO {
        private Long productoId;
        private Integer cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrestamoLineaDTO {
        private Long productoId;
    }
}
