package com.api_caich.dto.usuario;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioDTO {
    private Long usuarioId;
    private String nombreCompleto;
    private String email;
    private String folio;
    private String idAlumno;
    private String telefono;
    private String rol;
    private String estado;
    private String foto;
    private LocalDateTime ultimaConexion;
    private LocalDateTime fechaCreacion;
}