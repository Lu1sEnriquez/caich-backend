package com.api_caich.dto.reserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaAlumnoResponseDTO {

    private Long ticketId;
    private String folio;
    private String estado;
    private String mensaje;
}
