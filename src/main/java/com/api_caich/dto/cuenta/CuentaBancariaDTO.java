package com.api_caich.dto.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaBancariaDTO {
    private Long cuentaId;

    @NotBlank(message = "El banco es obligatorio")
    @Size(max = 100, message = "El banco no puede tener más de 100 caracteres")
    private String banco;

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Size(max = 50, message = "El número de cuenta no puede tener más de 50 caracteres")
    private String numeroCuenta;

    @Size(max = 18, message = "La CLABE no puede tener más de 18 caracteres")
    private String clabe;

    @NotBlank(message = "El titular es obligatorio")
    @Size(max = 200, message = "El titular no puede tener más de 200 caracteres")
    private String titular;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean estaActiva;
}
