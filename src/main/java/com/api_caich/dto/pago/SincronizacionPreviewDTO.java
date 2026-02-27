package com.api_caich.dto.pago;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el resultado completo de una sincronización de Google Forms
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionPreviewDTO {
    
    /**
     * Total de registros encontrados en el CSV
     */
    private Integer totalRegistros;
    
    /**
     * Lista de pagos que pueden ser sincronizados sin errores
     */
    private List<PagoSincronizacionDTO> pagosValidos;
    
    /**
     * Lista de pagos que tienen errores de validación
     */
    private List<PagoSincronizacionDTO> pagosConError;
    
    /**
     * Número de pagos válidos
     */
    private Integer totalValidos;
    
    /**
     * Número de pagos con error
     */
    private Integer totalErrores;
    
    /**
     * Número de pagos ya sincronizados (duplicados)
     */
    private Integer totalDuplicados;
}
