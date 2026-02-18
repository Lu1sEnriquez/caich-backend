package com.api_caich.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.MetodoPago;

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

@Getter
@Setter
@Entity
@Table(name = "Pagos")
public class Pago extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pagoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId", nullable = false)
    private Ticket ticket;

    @Column(name = "Monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "MetodoPago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "ComprobanteUrl", length = 500)
    private String comprobanteUrl; // URL de la foto del voucher

    @Column(name = "ReferenciaBancaria", length = 100)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false)
    private EstadoPagoTransaccion estado = EstadoPagoTransaccion.PENDIENTE_REVISION;

    @Column(name = "MotivoRechazo")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VerificadoPor")
    private Usuario verificadoPor;

    @Column(name = "FechaPago")
    private LocalDateTime fechaPago = LocalDateTime.now();
}
