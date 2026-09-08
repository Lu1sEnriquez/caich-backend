package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "Configuraciones")
@Data
public class Configuracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;

    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    private LocalDateTime ultimaActualizacion;
}