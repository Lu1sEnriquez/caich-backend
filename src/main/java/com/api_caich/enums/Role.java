package com.api_caich.enums;

import lombok.Getter;

@Getter

public enum Role {

    // Roles principales del sistema
    ADMINISTRADOR("Administrador", "ROLE_ADMIN", "Acceso completo al sistema"),
    TERAPEUTA("Terapeuta", "ROLE_TERAPEUTA", "Gestión de citas y pacientes"),
    ALUMNO("Alumno", "ROLE_ALUMNO", "Acceso a préstamos de materiales"),
    PACIENTE("Paciente", "ROLE_PACIENTE", "Acceso a sus citas e información"),
    TESORERIA("Tesorería", "ROLE_TESORERIA", "Gestión y validación de pagos en ventanilla");

    private final String nombre;
    private final String authority;
    private final String descripcion;

    Role(String nombre, String authority, String descripcion) {
        this.nombre = nombre;
        this.authority = authority;
        this.descripcion = descripcion;
    }

    /**
     * Obtiene el valor del rol sin el prefijo "ROLE_"
     * Útil para usar con .hasRole() de Spring Security
     * @return El valor del rol (ej: "ADMIN", "TERAPEUTA")
     */
    public String getRoleValue() {
        return this.authority.replace("ROLE_", "");
    }

    // Método para buscar por nombre
    public static Role fromNombre(String nombre) {
        for (Role role : values()) {
            if (role.nombre.equalsIgnoreCase(nombre)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no encontrado: " + nombre);
    }

    // Método para buscar por authority
    public static Role fromAuthority(String authority) {
        for (Role role : values()) {
            if (role.authority.equals(authority)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Authority no encontrada: " + authority);
    }

    // Método para verificar si un rol existe
    public static boolean isValid(String nombre) {
        for (Role role : values()) {
            if (role.nombre.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    // Método para obtener todos los nombres
    public static String[] getNombres() {
        return java.util.Arrays.stream(values())
                .map(Role::getNombre)
                .toArray(String[]::new);
    }

    // Método para obtener authorities
    public static String[] getAuthorities() {
        return java.util.Arrays.stream(values())
                .map(Role::getAuthority)
                .toArray(String[]::new);
    }
}
