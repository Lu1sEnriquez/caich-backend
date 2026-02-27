package com.api_caich.dto.pago;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el resultado final de una sincronización confirmada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionResultadoDTO {
    
    /**
     * Número de pagos sincronizados exitosamente
     */
    private Integer totalSincronizados;
    
    /**
     * Lista de folios sincronizados exitosamente
     */
    private List<String> foliosSincronizados;
    
    /**
     * Número de pagos que fallaron durante la sincronización
     */
    private Integer totalFallidos;
    
    /**
     * Lista de errores en caso de fallar algún registro
     */
    private List<ErrorSincronizacionDTO> errores;
    
    /**
     * Mensaje general del resultado
     */
    private String mensaje;
}
