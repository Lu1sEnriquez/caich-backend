package com.api_caich.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoRecargaMonedero;
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

/**
 * Solicitud de recarga al monedero realizada por un alumno.
 * El alumno sube su comprobante bancario; el admin aprueba o rechaza.
 * Si se rechaza, puede volver a intentar con un comprobante corregido.
 */
@Getter
@Setter
@Entity
@Table(name = "Recargas_Monedero")
public class RecargaMonedero extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RecargaMonederoId")
    private Long recargaMonederoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AlumnoId", nullable = false)
    private Usuario alumno;

    @Column(name = "Monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** URL o path del comprobante bancario subido por el alumno. */
    @Column(name = "ComprobanteUrl", length = 500)
    private String comprobanteUrl;

    /** Referencia bancaria / número de operación (opcional). */
    @Column(name = "ReferenciaBancaria", length = 100)
    private String referenciaBancaria;

    /** Nota del alumno (banco, tipo de pago, etc.). */
    @Column(name = "NotasAlumno", length = 500)
    private String notasAlumno;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false, length = 25)
    private EstadoRecargaMonedero estado = EstadoRecargaMonedero.PENDIENTE_REVISION;

    @Column(name = "MotivoRechazo", length = 500)
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VerificadoPor")
    private Usuario verificadoPor;

    @Column(name = "FechaSolicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "FechaVerificacion")
    private LocalDateTime fechaVerificacion;

    /** Intento #n del alumno (para rastrear múltiples intentos tras rechazo). */
    @Column(name = "NumeroIntento", nullable = false)
    private Integer numeroIntento = 1;

    /** Folio generado para recarga por tesorería. Null para otros métodos. */
    @Column(name = "Folio", length = 20)
    private String folio;

    /** Método de pago: TRANSFERENCIA, TESORERIA, etc. */
    @Enumerated(EnumType.STRING)
    @Column(name = "MetodoPago", nullable = false, length = 20)
    private MetodoPago metodoPago = MetodoPago.TRANSFERENCIA;

    /** Concepto de pago (motivo, referencia para ticket). */
    @Column(name = "Concepto", length = 200)
    private String concepto;
}
