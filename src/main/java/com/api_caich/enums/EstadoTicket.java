package com.api_caich.enums;

public enum EstadoTicket {
    BORRADOR, // Se está creando
    AGENDADO, // Cita confirmada / Espacio reservado
    EN_PROGRESO, // El servicio está ocurriendo
    COMPLETADO, // Ya se entregó el producto o terminó la cita
    NO_ASISTIO, // El personal confirmó que la persona no acudió
    CANCELADO       // Se anuló
}
