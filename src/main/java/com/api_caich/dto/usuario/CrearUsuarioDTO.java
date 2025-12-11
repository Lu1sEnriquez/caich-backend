package com.api_caich.dto.usuario;

import com.api_caich.validation.ConditionalAlumnoId;
import com.api_caich.validation.ValidRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@ConditionalAlumnoId()
@Data
public class CrearUsuarioDTO {

    @NotBlank(message = "Nombre completo es requerido")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "Email es requerido")
    @Email(message = "El formato del email no es válido")
    private String email;

    private String folio;
   
    private String idAlumno;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Pattern(regexp = "\\d{10}", message = "El telefono debe tener 10 digitos")
    private String telefono;

    @NotBlank(message = "El rol es requerido")
    @ValidRole(includeAdmin = true, message = "Rol no valido")
    private String rol;

    private String foto;
}