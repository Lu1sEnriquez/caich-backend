package com.api_caich.dto.sancion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotivoSancionDeleteResponseDTO {
    private Long motivoSancionId;
    private boolean eliminado;
}
