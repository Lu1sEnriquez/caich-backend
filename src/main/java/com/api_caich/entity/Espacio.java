package com.api_caich.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

}
