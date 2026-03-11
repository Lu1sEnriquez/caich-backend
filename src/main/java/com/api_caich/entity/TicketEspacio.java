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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Ticket_Espacios")
public class TicketEspacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspacioId", nullable = false)
    private Espacio espacio;

    @Column(name = "HoraInicioReserva", nullable = false)
    private LocalDateTime horaInicioReserva;

    @Column(name = "HoraFinReserva", nullable = false)
    private LocalDateTime horaFinReserva;

    // Costo por usar el espacio (puede ser 0 si es alumno)
    @Column(name = "CostoCobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoCobrado;

    @Column(name = "Celular", length = 20)
    private String celular;

    @Column(name = "Actividad", length = 255)
    private String actividad;

    @Column(name = "Maestro", length = 255)
    private String maestro;
}
