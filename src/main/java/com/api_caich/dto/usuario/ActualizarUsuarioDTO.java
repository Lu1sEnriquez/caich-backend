package com.api_caich.dto.usuario;

import com.api_caich.validation.ValidRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarUsuarioDTO {

    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String nombreCompleto;

    @Email(message = "El formato del email no es válido")
    private String email;

    private String folio;

    private String idAlumno;

    @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    @ValidRole(includeAdmin = true, message = "Rol no valido")
    private String rol;

    private String foto;
}