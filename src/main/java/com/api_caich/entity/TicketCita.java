package com.api_caich.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@Entity
@Table(name = "Ticket_Citas")
public class TicketCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PacienteId")
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TerapeutaId")
    private Usuario terapeuta;

    @Column(name = "FechaInicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "FechaFin", nullable = false)
    private LocalDateTime fechaFin;

    // Precio específico de ESTA sesión (puede variar del estándar)
    @Column(name = "PrecioCobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCobrado;

    @Column(name = "NotasTerapia", columnDefinition = "TEXT")
    private String notasTerapia;

    @Column(name = "Modalidad", length = 50, nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'Presencial'")
    private String modalidad = "Presencial"; // Presencial u Online
}
