package com.api_caich.dto.espacio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEspacioDTO {
    private String nombre;
    private String tipo;
    private String descripcion;
    private Integer capacidad;
    private BigDecimal costoPorHora;
    private Boolean estaActivo;
}
