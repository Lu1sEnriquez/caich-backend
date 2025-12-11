package com.api_caich.dto.ticket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoDetalleDTO {

    @NotNull(message = "Producto ID is required")
    private Long productoId;

    @NotNull(message = "Cantidad is required")
    @Min(value = 1, message = "Cantidad must be at least 1")
    private Integer cantidad;

    @NotNull(message = "Precio unitario is required")
    private BigDecimal precioUnitario;

    @NotNull(message = "Tipo uso is required")
    private String tipoUso; // "Venta", "Prestamo", "Uso"
}