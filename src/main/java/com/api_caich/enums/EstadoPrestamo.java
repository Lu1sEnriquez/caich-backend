package com.api_caich.enums;

/**
 * Estados posibles para un préstamo de producto
 */
public enum EstadoPrestamo {
    ACTIVO("Activo"),
    DEVUELTO("Devuelto"),
    PERDIDO("Perdido"),
    VENCIDO("Vencido");

    private final String descripcion;

    EstadoPrestamo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
