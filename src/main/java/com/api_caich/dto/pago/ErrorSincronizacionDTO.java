package com.api_caich.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un error de sincronización
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorSincronizacionDTO {
    
    /**
     * Folio del ticket relacionado
     */
    private String folio;
    
    /**
     * Número de fila en el CSV
     */
    private Integer numeroFila;
    
    /**
     * Descripción del error
     */
    private String error;
}
