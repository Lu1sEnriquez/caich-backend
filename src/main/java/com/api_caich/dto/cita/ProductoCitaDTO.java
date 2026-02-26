package com.api_caich.dto.cita;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoCitaDTO {

    private Long productoId;

    private Integer cantidad;

    private BigDecimal precioUnitario;

    private String tipoUso; // Venta | Prestamo | Uso

    private String fechaDevolucionEstimada; // ISO date-time for loans

    private String observaciones;
}
