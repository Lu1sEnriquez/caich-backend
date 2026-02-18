package com.api_caich.enums;

public enum EstadoTicket {
    BORRADOR, // Se está creando
    AGENDADO, // Cita confirmada / Espacio reservado
    EN_PROGRESO, // El servicio está ocurriendo
    COMPLETADO, // Ya se entregó el producto o terminó la cita
    CANCELADO       // Se anuló
}
