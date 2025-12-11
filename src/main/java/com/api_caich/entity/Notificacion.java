package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Notificaciones")
public class Notificacion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificacionId")
    private Long notificacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UsuarioId", nullable = false)
    private Usuario usuario;

    @Column(name = "Tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "Titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "Mensaje", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String mensaje;

    @Column(name = "Leida", nullable = false)
    private Boolean leida = false;

    @Column(name = "FechaLeida")
    private LocalDateTime fechaLeida;

    @Column(name = "ReferenciaId")
    private Long referenciaId;

    @Column(name = "ReferenciaUrl", length = 300)
    private String referenciaUrl;
}