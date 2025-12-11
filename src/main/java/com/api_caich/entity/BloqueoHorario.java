package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "BloqueoHorarios")
public class BloqueoHorario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BloqueoId")
    private Long bloqueoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspacioId")
    private Espacio espacio; // NULL = bloqueo global

    @Column(name = "FechaInicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "FechaFin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "Motivo", length = 300)
    private String motivo;

    @Column(name = "TodoElDia", nullable = false)
    private Boolean todoElDia = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreadoPor")
    private Usuario creadoPor;
}