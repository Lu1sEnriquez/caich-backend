package com.api_caich.dto.horario;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO que representa un slot de tiempo disponible.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotDTO {

    @JsonProperty("inicio")
    private LocalDateTime inicio;

    @JsonProperty("fin")
    private LocalDateTime fin;

    @Builder.Default
    @JsonProperty("disponible")
    private Boolean disponible = true;

    @JsonProperty("motivo")
    private String motivo; // Razón si no está disponible (ej: "Reservado", "Mantenimiento")
}
