package com.api_caich.dto.prestamo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrestamoDTO {

    private Long prestamoId;
    private String alumnoNombre;
    private String productoNombre;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionEstimada;
    private LocalDateTime fechaDevolucionReal;
    private String estadoPrestamo;
    
    // NUEVO: Costo unitario del préstamo (puede ser NULL si es gratis)
    private BigDecimal costoUnitario;
    
    // NUEVO: Observaciones de la devolución
    private String observacionesDevolucion;

    public Integer getDiasVencidos() {
        if ("Activo".equals(estadoPrestamo) && fechaDevolucionEstimada != null) {
            return (int) ChronoUnit.DAYS.between(fechaDevolucionEstimada, LocalDateTime.now());
        }
        return 0;
    }
}
