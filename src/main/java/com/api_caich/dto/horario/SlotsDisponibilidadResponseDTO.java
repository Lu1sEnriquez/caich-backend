package com.api_caich.dto.horario;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que contiene los slots disponibles junto con la configuración aplicada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotsDisponibilidadResponseDTO {
    
    /**
     * Información de la configuración aplicada.
     */
    private ConfiguracionInfoDTO configuracion;
    
    /**
     * Lista de slots con su estado de disponibilidad.
     */
    private List<SlotDisponibilidadDTO> slots;
    
    /**
     * Resumen de la disponibilidad.
     */
    private ResumenDisponibilidadDTO resumen;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfiguracionInfoDTO {
        private Long espacioId;
        private String espacioNombre;
        private LocalDate fecha;
        private String diaSemana;
        private String horaApertura;
        private String horaCierre;
        private Integer intervaloMinutos;
        private Integer duracionMinimaMinutos;
        private Integer duracionMaximaMinutos;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenDisponibilidadDTO {
        private Integer totalSlots;
        private Integer disponibles;
        private Integer ocupados;
        private Integer bloqueados;
    }
}
