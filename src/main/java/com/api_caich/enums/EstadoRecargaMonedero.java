package com.api_caich.enums;

public enum EstadoRecargaMonedero {
    PENDIENTE_REVISION,      // El alumno subió su comprobante, espera aprobación
    APROBADO,                // El admin validó el pago y acreditó el saldo
    RECHAZADO,               // El comprobante no fue aceptado; el alumno puede reintentar
    PENDIENTE_TESORERIA,     // El alumno generó folio y debe ir a tesorería a pagar
    CONFIRMADO_TESORERIA     // Tesorería validó el pago y lo confirmó
}
