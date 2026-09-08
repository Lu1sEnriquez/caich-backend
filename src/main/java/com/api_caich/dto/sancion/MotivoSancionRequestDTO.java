package com.api_caich.dto.sancion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotivoSancionRequestDTO {

    @NotBlank(message = "La descripcion es requerida")
    @Size(max = 200, message = "La descripcion no puede exceder 200 caracteres")
    private String descripcion;
}
