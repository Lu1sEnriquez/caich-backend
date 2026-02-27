package com.api_caich.dto.prestamo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearPrestamoDTO {

    private Long ticketId;

    @NotNull(message = "Alumno ID is required")
    private Long alumnoId;

    @NotNull(message = "Producto ID is required")
    private Long productoId;

    @NotNull(message = "Fecha prestamo is required")
    private LocalDateTime fechaPrestamo;

    @NotNull(message = "Fecha devolucion estimada is required")
    @FutureOrPresent(message = "Fecha devolucion estimada must be now or in the future")
    private LocalDateTime fechaDevolucionEstimada;

    @NotNull(message = "Cantidad is required")
    @Min(value = 1, message = "Cantidad must be at least 1")
    private Integer cantidad;

    private String observaciones;
    
    // NUEVO: Costo unitario del préstamo (opcional, NULL = gratis)
    private BigDecimal costoUnitario;
}
