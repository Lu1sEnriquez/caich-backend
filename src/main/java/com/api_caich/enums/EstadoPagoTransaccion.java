package com.api_caich.enums;

public enum EstadoPagoTransaccion {
    PENDIENTE_REVISION, // El usuario subió la foto
    APROBADO, // El admin validó que entró el dinero
    RECHAZADO           // La foto no es legible o no cayó el pago
}
