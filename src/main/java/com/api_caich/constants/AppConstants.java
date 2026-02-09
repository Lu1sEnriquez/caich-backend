package com.api_caich.constants;

/**
 * Constantes generales de la aplicación
 */
public final class AppConstants {
    
    private AppConstants() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no debe ser instanciada");
    }

    // Paginación
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String DEFAULT_SORT_BY = "nombreCompleto";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    
    // Tokens de tiempo (en segundos)
    public static final long JWT_EXPIRATION_SECONDS = 86400L; // 24 horas
    public static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7L;
    
    // Archivos
    public static final long MAX_FILE_SIZE = 5242880L; // 5MB
    public static final String UPLOAD_DIR_PROFILES = "/uploads/profiles/";
    
    // Timezone
    public static final String DEFAULT_TIMEZONE = "America/Mexico_City";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
