package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PrestamosMateriales")
public class PrestamoMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PrestamoId")
    private Long prestamoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TicketId")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AlumnoId", nullable = false)
    private Usuario alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductoId", nullable = false)
    private Producto producto;

    @Column(name = "FechaPrestamo", nullable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "FechaDevolucionEstimada", nullable = false)
    private LocalDateTime fechaDevolucionEstimada;

    @Column(name = "FechaDevolucionReal")
    private LocalDateTime fechaDevolucionReal;

    @Column(name = "Cantidad", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "EstadoPrestamo", nullable = false, length = 20)
    private String estadoPrestamo = "Activo";

    @Column(name = "Observaciones", columnDefinition = "NVARCHAR(MAX)")
    private String observaciones;
}