package com.api_caich.dto.monedero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MovimientoMonederoDTO {
    private Long movimientoId;
    private String tipo;           // TipoMovimientoMonedero.name()
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;
    private String descripcion;
    private Long ticketId;
    private String ticketFolio;
    private Long recargaMonederoId;
    private LocalDateTime fechaMovimiento;

    // Solo en contexto admin
    private Long alumnoId;
    private String alumnoNombre;
}
