package com.api_caich.dto.horario;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un slot de tiempo individual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDisponibilidadDTO {
    
    private LocalTime horaInicio;
    
    private LocalTime horaFin;
    
    /**
     * Estado del slot: DISPONIBLE, OCUPADO, BLOQUEADO
     */
    private String estado;
    
    /**
     * Indica si el slot está disponible para reserva.
     */
    private Boolean disponible;
    
    /**
     * Motivo por el cual no está disponible (si aplica).
     */
    private String motivo;
    
    /**
     * ID de la cita que ocupa este slot (si aplica).
     */
    private Long citaId;
}
