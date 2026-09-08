package com.api_caich.dto.sancion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.api_caich.enums.EstadoSancion;
import com.api_caich.enums.TipoSancion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SancionDTO {

    private Long sancionId;
    private Long alumnoId;
    private String alumnoNombre;
    private TipoSancion tipoSancion;
    private String motivo;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoSancion estado;
    private String creadoPorNombre;
    private LocalDateTime fechaCreacion;
    private String anuladoPorNombre;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;
    private Long ticketRelacionadoId;
    private String ticketFolio;
}
