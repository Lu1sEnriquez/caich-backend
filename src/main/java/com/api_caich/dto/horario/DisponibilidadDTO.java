package com.api_caich.dto.horario;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO que contiene los slots disponibles para una fecha y espacio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadDTO {

    @JsonProperty("espacioId")
    private Long espacioId;

    @JsonProperty("fecha")
    private LocalDate fecha;

    @JsonProperty("horarioBase")
    private HorarioBaseDTO horarioBase;

    @JsonProperty("horaApertura")
    private String horaApertura;

    @JsonProperty("horaCierre")
    private String horaCierre;

    @JsonProperty("diasOperativo")
    private Boolean diasOperativo;

    @JsonProperty("abierto")
    private Boolean abierto; // false si está completamente cerrado

    @JsonProperty("slots")
    private List<SlotDTO> slots;

    @JsonProperty("totalSlots")
    private Integer totalSlots;

    @JsonProperty("totalSlotsDisponibles")
    private Integer totalSlotsDisponibles;

    @JsonProperty("mensaje")
    private String mensaje; // Mensaje informativo si aplica (ej: "Cerrado", "Mantenimiento")
}
