package com.api_caich.enums;

public enum MetodoPago {
    EFECTIVO,
    TRANSFERENCIA,
    DEPOSITO,
    TARJETA,
    MONEDERO,   // Pago descontado directamente del monedero virtual del alumno
    TESORERIA   // Pago realizado en ventanilla de tesorería (requiere folio y validación)
}
