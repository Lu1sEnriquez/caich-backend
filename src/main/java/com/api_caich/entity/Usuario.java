package com.api_caich.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Usuarios")
public class Usuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UsuarioId")
    private Long usuarioId;

    @Column(name = "NombreCompleto", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "Folio", length = 50)
    private String folio;

    @Column(name = "idAlumno", length = 50) //ID del ITSON
    private String idAlumno;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "Telefono", length = 20)
    private String telefono;

    @Column(name = "Rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "Estado", nullable = false, length = 20)
    private String estado = com.api_caich.enums.EstadoUsuario.ACTIVO.getValor();

    @Column(name = "Foto", length = 500)
    private String foto;

    @Column(name = "UltimaConexion")
    private LocalDateTime ultimaConexion;

    @OneToMany(mappedBy = "paciente")
    private List<Ticket> ticketsPaciente;

    @OneToMany(mappedBy = "terapeuta")
    private List<Ticket> ticketsTerapeuta;

    @OneToMany(mappedBy = "creadoPor")
    private List<Ticket> ticketsCreados;
}
