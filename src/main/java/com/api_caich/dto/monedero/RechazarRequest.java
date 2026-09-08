package com.api_caich.dto.monedero;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RechazarRequest {

    @NotBlank(message = "El motivo de rechazo es requerido")
    private String motivo;
}
