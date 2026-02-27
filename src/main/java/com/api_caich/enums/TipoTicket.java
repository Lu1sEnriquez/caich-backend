package com.api_caich.enums;

public enum TipoTicket {
    CITA("Cita de Terapia"),
    PRESTAMO_CUBICULO("Préstamo de Cubículo"),
    PRESTAMO_PRODUCTO("Préstamo de Producto"),
    VENTA_PRODUCTO("Venta de Producto");

    private final String descripcion;

    TipoTicket(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
