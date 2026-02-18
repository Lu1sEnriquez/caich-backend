package com.api_caich.dto.ticket;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPrestamoDTO {

    private Long prestamoId;

    @NotNull(message = "El producto ID es requerido")
    private Long productoId;

    private String productoNombre;

    @NotNull(message = "El responsable ID es requerido")
    private Long responsableId;

    private String responsableNombre;

    @NotNull(message = "La fecha de préstamo es requerida")
    private LocalDateTime fechaPrestamo;

    @NotNull(message = "La fecha de devolución estimada es requerida")
    private LocalDateTime fechaDevolucionEstimada;

    private LocalDateTime fechaDevolucionReal;

    private String estado;
}
