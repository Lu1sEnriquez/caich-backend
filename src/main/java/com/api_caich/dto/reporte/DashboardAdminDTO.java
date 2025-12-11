package com.api_caich.dto.reporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardAdminDTO {
    private Integer citasHoy;
    private Integer pagosPendientes;
    private Integer nuevosPacientes;
    private Integer citasEsteMes;
    private Integer prestamosActivos;
    private Integer prestamosVencidos;
    private Integer productosStockBajo;
    private Integer productosSinStock;
    private BigDecimal ingresosMes;
}