package com.api_caich.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoSancion;
import com.api_caich.enums.TipoSancion;

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
@Table(name = "Sanciones")
public class Sancion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sancionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AlumnoId", nullable = false)
    private Usuario alumno;

    @Enumerated(EnumType.STRING)
    @Column(name = "TipoSancion", nullable = false)
    private TipoSancion tipoSancion;

    @Column(name = "Motivo", nullable = false, length = 1000)
    private String motivo;

    @Column(name = "Descripcion", length = 2000)
    private String descripcion;

    @Column(name = "FechaInicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FechaFin")
    private LocalDate fechaFin; // null para sanciones permanentes o advertencias

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false)
    private EstadoSancion estado = EstadoSancion.ACTIVA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreadoPor", nullable = false)
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AnuladoPor")
    private Usuario anuladoPor;

    @Column(name = "FechaAnulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "MotivoAnulacion", length = 500)
    private String motivoAnulacion;

    // Relación con ticket si la sanción es por un ticket específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketRelacionado")
    private Ticket ticketRelacionado;
}
