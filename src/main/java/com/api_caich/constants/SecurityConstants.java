package com.api_caich.constants;

/**
 * Constantes relacionadas con seguridad y autenticación
 * Centraliza valores comunes para evitar strings mágicos
 */
public final class SecurityConstants {
    
    private SecurityConstants() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no debe ser instanciada");
    }

    // JWT Token
    public static final String TOKEN_TYPE = "Bearer";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    
    // OpenAPI Security
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";
    
    // Mensajes de error
    public static final String ERROR_INVALID_CREDENTIALS = "Credenciales inválidas";
    public static final String ERROR_INVALID_TOKEN = "Invalid or expired token";
    public static final String ERROR_ACCESS_DENIED = "Access denied";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed: ";
    public static final String ERROR_UNEXPECTED = "An unexpected error occurred";
    
    // Mensajes de éxito
    public static final String SUCCESS_LOGIN = "Login successful";
    public static final String SUCCESS_LOGOUT = "Sesión cerrada exitosamente";
    public static final String SUCCESS_LOGOUT_ALL = "Logged out from all devices";
    public static final String SUCCESS_REGISTER = "Registration successful";
    public static final String SUCCESS_TOKEN_REFRESH = "Token refreshed successfully";
}
