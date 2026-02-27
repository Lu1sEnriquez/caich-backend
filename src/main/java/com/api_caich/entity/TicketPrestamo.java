package com.api_caich.entity;

import java.time.LocalDateTime;

import com.api_caich.enums.EstadoPrestamo;

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
@Table(name = "Ticket_Prestamos")
public class TicketPrestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductoId", nullable = false)
    private Producto producto; // Debe tener esPrestable = true

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ResponsableId", nullable = false)
    private Usuario responsable; // Alumno o Terapeuta

    @Column(name = "FechaPrestamo", nullable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "FechaDevolucionEstimada", nullable = false)
    private LocalDateTime fechaDevolucionEstimada;

    @Column(name = "FechaDevolucionReal")
    private LocalDateTime fechaDevolucionReal;

    @Column(name = "Estado", length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;

    @Column(name = "CostoUnitario", precision = 10, scale = 2)
    private java.math.BigDecimal costoUnitario; // NULL = Gratis, si tiene valor = cobrable

    @Column(name = "ObservacionesDevolucion", length = 500)
    private String observacionesDevolucion; // Notas al devolver el préstamo
}
