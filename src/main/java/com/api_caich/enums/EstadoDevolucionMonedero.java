package com.api_caich.enums;

public enum EstadoDevolucionMonedero {
    PENDIENTE,  // Ticket cancelado; espera que el admin apruebe la devolución
    APROBADA,   // Admin aprobó; el saldo fue reintegrado al monedero
    RECHAZADA   // Admin rechazó la devolución (ej. cancelación fuera de política)
}
