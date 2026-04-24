package com.api_caich.enums;

public enum EstadoRecargaMonedero {
    PENDIENTE_REVISION, // El alumno subió su comprobante, espera aprobación
    APROBADO,           // El admin validó el pago y acreditó el saldo
    RECHAZADO           // El comprobante no fue aceptado; el alumno puede reintentar
}
