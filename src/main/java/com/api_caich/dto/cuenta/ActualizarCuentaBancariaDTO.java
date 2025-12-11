package com.api_caich.dto.cuenta;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarCuentaBancariaDTO {
    @Size(max = 100, message = "El banco no puede tener más de 100 caracteres")
    private String banco;

    @Size(max = 50, message = "El número de cuenta no puede tener más de 50 caracteres")
    private String numeroCuenta;

    @Size(max = 18, message = "La CLABE no puede tener más de 18 caracteres")
    private String clabe;

    @Size(max = 200, message = "El titular no puede tener más de 200 caracteres")
    private String titular;

    private Boolean estaActiva;
}
