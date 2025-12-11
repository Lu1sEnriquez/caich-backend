package com.api_caich.dto.auditoria;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistorialCambioDTO {
    private Long cambioId;
    private String entidad;
    private Long entidadId;
    private String accion;
    private String camposModificados;
    private String valoresAnteriores;
    private String valoresNuevos;
    private String usuarioNombre;
    private LocalDateTime fechaCambio;
    private String direccionIP;
}