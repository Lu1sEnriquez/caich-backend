package com.api_caich.dto.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfiguracionRequestDTO {

    @NotBlank
    private String clave;

    @NotBlank
    private String valor;

    private String descripcion;
}
