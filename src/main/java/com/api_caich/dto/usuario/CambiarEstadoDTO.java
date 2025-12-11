package com.api_caich.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CambiarEstadoDTO {

    @NotBlank(message = "El estado es requerido")
    @Pattern(regexp = "^(Activo|Suspendido|Inactivo)$", message = "Estado no valido. Debe ser: Activo, Suspendido o Inactivo")
    private String estado;

    private String motivo;
}
