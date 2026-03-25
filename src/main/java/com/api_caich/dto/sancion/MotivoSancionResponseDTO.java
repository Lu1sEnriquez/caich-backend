package com.api_caich.dto.sancion;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotivoSancionResponseDTO {
    private Long motivoSancionId;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;
}
