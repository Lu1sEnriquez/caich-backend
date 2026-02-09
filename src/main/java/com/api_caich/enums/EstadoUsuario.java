package com.api_caich.enums;

import lombok.Getter;

@Getter
public enum EstadoUsuario {
    
    ACTIVO("Activo", "Usuario activo en el sistema"),
    SUSPENDIDO("Suspendido", "Usuario suspendido temporalmente"),
    INACTIVO("Inactivo", "Usuario inactivo");

    private final String valor;
    private final String descripcion;

    EstadoUsuario(String valor, String descripcion) {
        this.valor = valor;
        this.descripcion = descripcion;
    }

    /**
     * Buscar estado por su valor string
     * @param valor El valor del estado (ej: "Activo")
     * @return El enum correspondiente
     */
    public static EstadoUsuario fromValor(String valor) {
        for (EstadoUsuario estado : values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de usuario no válido: " + valor);
    }

    /**
     * Verifica si un valor es un estado válido
     * @param valor El valor a verificar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValid(String valor) {
        for (EstadoUsuario estado : values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene todos los valores posibles
     * @return Array con todos los valores
     */
    public static String[] getValores() {
        return java.util.Arrays.stream(values())
                .map(EstadoUsuario::getValor)
                .toArray(String[]::new);
    }
}
