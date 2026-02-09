package com.api_caich.dto.usuario;

import com.api_caich.enums.EstadoUsuario;
import com.api_caich.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarEstadoDTO {

    @NotBlank(message = "El estado es requerido")
    @ValidEnum(enumClass = EstadoUsuario.class, message = "Estado no válido. Valores permitidos: Activo, Suspendido, Inactivo")
    private String estado;

    private String motivo;
}
