package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "MovimientosInventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovimientoId")
    private Long movimientoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductoId", nullable = false)
    private Producto producto;

    @Column(name = "TipoMovimiento", nullable = false, length = 20)
    private String tipoMovimiento;

    @Column(name = "Cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "StockAnterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "StockNuevo", nullable = false)
    private Integer stockNuevo;

    @Column(name = "Motivo", length = 200)
    private String motivo;

    @Column(name = "Referencia", length = 100)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RealizadoPor")
    private Usuario realizadoPor;

    @Column(name = "FechaMovimiento", nullable = false)
    private LocalDateTime fechaMovimiento = LocalDateTime.now();
}