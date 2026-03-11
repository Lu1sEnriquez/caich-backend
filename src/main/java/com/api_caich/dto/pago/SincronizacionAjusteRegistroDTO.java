package com.api_caich.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionAjusteRegistroDTO {
    private String marcaTemporal;
    private String email;
    private String folio;
    private String nombrePaciente;
    private String monto;
    private String metodoPago;
    private String referencia;
    private String comprobanteUrl;
}
