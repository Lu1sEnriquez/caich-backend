package com.api_caich.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.TipoMovimientoMonedero;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Registro inmutable de cada operación que modifica el saldo del monedero.
 * Nunca debe eliminarse: es el libro mayor de la billetera.
 */
@Getter
@Setter
@Entity
@Table(name = "Movimientos_Monedero")
public class MovimientoMonedero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovimientoId")
    private Long movimientoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MonederoId", nullable = false)
    private Monedero monedero;

    @Enumerated(EnumType.STRING)
    @Column(name = "Tipo", nullable = false, length = 30)
    private TipoMovimientoMonedero tipo;

    @Column(name = "Monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "SaldoAnterior", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "SaldoNuevo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoNuevo;

    @Column(name = "Descripcion", length = 500)
    private String descripcion;

    /** Ticket relacionado (presente en CONSUMO y DEVOLUCION_CANCELACION). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId")
    private Ticket ticket;

    /** Recarga relacionada (presente en RECARGA). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RecargaMonederoId")
    private RecargaMonedero recargaMonedero;

    @Column(name = "FechaMovimiento", nullable = false)
    private LocalDateTime fechaMovimiento = LocalDateTime.now();
}
