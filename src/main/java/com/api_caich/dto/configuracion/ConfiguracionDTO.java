package com.api_caich.dto.configuracion;

import java.time.LocalDateTime;

import com.api_caich.entity.Configuracion;

import lombok.Data;

@Data
public class ConfiguracionDTO {
    private Integer id;
    private String clave;
    private String valor;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime ultimaActualizacion;

    public static ConfiguracionDTO fromEntity(Configuracion configuracion) {
        ConfiguracionDTO dto = new ConfiguracionDTO();
        dto.setId(configuracion.getId());
        dto.setClave(configuracion.getClave());
        dto.setValor(configuracion.getValor());
        dto.setDescripcion(configuracion.getDescripcion());
        dto.setActivo(configuracion.getActivo());
        dto.setUltimaActualizacion(configuracion.getUltimaActualizacion());
        return dto;
    }
}
