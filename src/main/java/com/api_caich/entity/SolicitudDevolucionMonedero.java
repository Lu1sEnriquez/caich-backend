package com.api_caich.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoDevolucionMonedero;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Solicitud de devolución de saldo al monedero del alumno,
 * generada automáticamente cuando se cancela un ticket que
 * fue pagado con monedero (MetodoPago.MONEDERO).
 *
 * El administrador debe aprobar explícitamente la devolución.
 */
@Getter
@Setter
@Entity
@Table(name = "Solicitudes_Devolucion_Monedero")
public class SolicitudDevolucionMonedero extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SolicitudDevolucionId")
    private Long solicitudDevolucionId;

    /** Ticket que fue cancelado. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MonederoId", nullable = false)
    private Monedero monedero;

    @Column(name = "MontoDevolucion", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false, length = 20)
    private EstadoDevolucionMonedero estado = EstadoDevolucionMonedero.PENDIENTE;

    @Column(name = "MotivoRechazo", length = 500)
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProcesadoPor")
    private Usuario procesadoPor;

    @Column(name = "FechaSolicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "FechaProcesamiento")
    private LocalDateTime fechaProcesamiento;
}
