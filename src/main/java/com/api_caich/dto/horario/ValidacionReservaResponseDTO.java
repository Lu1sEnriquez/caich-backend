package com.api_caich.dto.horario;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la validación de reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionReservaResponseDTO {
    
    private Boolean esValido;
    
    private Integer duracionMinutos;
    
    private Integer slotsOcupados;
    
    private BigDecimal costoEstimado;
    
    /**
     * Lista de errores de validación (si esValido = false).
     */
    private List<ErrorValidacionDTO> errores;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorValidacionDTO {
        private String codigo;
        private String campo;
        private String mensaje;
        private String valorRecibido;
        private List<String> valoresValidos;
    }
}
