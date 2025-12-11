package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Espacios")
public class Espacio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EspacioId")
    private Long espacioId;

    @Column(name = "Nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "Tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "Descripcion", columnDefinition = "NVARCHAR(MAX)")
    private String descripcion;

    @Column(name = "Capacidad", nullable = false)
    private Integer capacidad = 1;

    @Column(name = "CostoPorHora", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPorHora = BigDecimal.ZERO;

    @Column(name = "EstaActivo", nullable = false)
    private Boolean estaActivo = true;

    @OneToMany(mappedBy = "espacio")
    private List<Ticket> tickets;
}