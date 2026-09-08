package com.api_caich.enums;

public enum TipoMovimientoMonedero {
    RECARGA,                  // El admin aprobó una recarga de saldo
    CONSUMO,                  // Se descontó saldo al confirmar una reserva
    DEVOLUCION_CANCELACION,   // Se reintegró saldo por cancelación de reserva aprobada
    AJUSTE_CREDITO            // Corrección manual de saldo por parte del admin (crédito)
}
