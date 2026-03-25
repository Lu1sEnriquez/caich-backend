package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "HistorialCambios")
public class HistorialCambios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CambioId")
    private Long cambioId;

    @Column(name = "Entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "EntidadId", nullable = false)
    private Long entidadId;

    @Column(name = "Accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "CamposModificados", columnDefinition = "TEXT")
    private String camposModificados;

    @Column(name = "ValoresAnteriores", columnDefinition = "TEXT")
    private String valoresAnteriores;

    @Column(name = "ValoresNuevos", columnDefinition = "TEXT")
    private String valoresNuevos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UsuarioId")
    private Usuario usuario;

    @Column(name = "FechaCambio", nullable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "DireccionIP", length = 45)
    private String direccionIP;
}