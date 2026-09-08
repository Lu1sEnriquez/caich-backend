package com.api_caich.dto.pago;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el resultado de procesar un registro individual del CSV
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoSincronizacionDTO {
    
    /**
     * Número de fila en el CSV
     */
    private Integer numeroFila;
    
    /**
     * Email del usuario que reportó el pago
     */
    private String email;
    
    /**
     * Folio del ticket
     */
    private String folio;
    
    /**
     * Nombre del paciente que reportó el pago
     */
    private String nombrePaciente;
    
    /**
     * Monto del pago
     */
    private BigDecimal monto;
    
    /**
     * Método de pago
     */
    private String metodoPago;
    
    /**
     * Referencia bancaria
     */
    private String referencia;
    
    /**
     * URL del comprobante en Google Drive
     */
    private String comprobanteUrl;
    
    /**
     * Notas adicionales del formulario
     */
    private String notasAdicionales;
    
    /**
     * Indica si el registro es válido para sincronización
     */
    private Boolean esValido;
    
    /**
     * Mensaje de error si no es válido
     */
    private String mensajeError;
    
    /**
     * ID del ticket asociado (si se encontró)
     */
    private Long ticketId;
    
    /**
     * Monto total del ticket
     */
    private BigDecimal montoTotalTicket;
    
    /**
     * Monto ya pagado del ticket
     */
    private BigDecimal montoPagadoTicket;
    
    /**
     * Saldo pendiente del ticket
     */
    private BigDecimal saldoPendienteTicket;
    
    /**
     * Indica si este pago ya fue sincronizado anteriormente
     */
    private Boolean esDuplicado;

    /**
     * Marca temporal de la respuesta del formulario (columna 0 del CSV).
     * Se usa como identificador único para detección de duplicados.
     */
    private String marcaTemporal;
}
