package com.api_caich.service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para interactuar con Google Drive API usando Service Account
 * Permite descargar archivos privados sin hacerlos públicos
 */
@Slf4j
@Service
public class GoogleDriveService {

    @Value("${google.drive.credentials.path:}")
    private String credentialsPath;

    @Value("${google.drive.application-name:CAICH API}")
    private String applicationName;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private Drive driveService;

    /**
     * Obtiene el servicio de Google Drive autenticado
     */
    private Drive getDriveService() throws Exception {
        if (driveService == null) {
            log.info("Inicializando servicio de Google Drive...");
            
            // Verificar que exista el archivo de credenciales
            if (credentialsPath == null || credentialsPath.isEmpty()) {
                throw new IllegalStateException(
                    "No se ha configurado la ruta de credenciales de Google Drive. " +
                    "Configura 'google.drive.credentials.path' en application.properties"
                );
            }

            Path credPath = Paths.get(credentialsPath);
            if (!Files.exists(credPath)) {
                throw new IllegalStateException(
                    "El archivo de credenciales de Google Drive no existe: " + credentialsPath
                );
            }

            // Cargar credenciales de Service Account
            GoogleCredentials credentials;
            try (InputStream in = new FileInputStream(credentialsPath)) {
                credentials = ServiceAccountCredentials.fromStream(in)
                        .createScoped(Collections.singletonList(DriveScopes.DRIVE_READONLY));
                
                log.info("Credenciales de Service Account cargadas exitosamente");
                
                // Mostrar info de la Service Account (para debug)
                if (credentials instanceof ServiceAccountCredentials) {
                    ServiceAccountCredentials saCredentials = (ServiceAccountCredentials) credentials;
                    log.info("Service Account Email: {}", saCredentials.getServiceAccountUser());
                }
            }

            // Crear servicio de Drive
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            driveService = new Drive.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();

            log.info("Servicio de Google Drive inicializado correctamente");
        }

        return driveService;
    }

    /**
     * Descarga un archivo de Google Drive por su ID
     * 
     * @param fileId ID del archivo en Google Drive
     * @return Contenido del archivo en bytes
     * @throws Exception Si hay error en la descarga
     */
    public byte[] downloadFile(String fileId) throws Exception {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del archivo no puede estar vacío");
        }

        log.info("Descargando archivo de Google Drive con ID: {}", fileId);

        try {
            Drive service = getDriveService();

            // Obtener información del archivo primero (para debug)
            try {
                com.google.api.services.drive.model.File fileMetadata = service.files()
                        .get(fileId)
                        .setFields("name, mimeType, size")
                        .execute();

                log.info("Archivo encontrado:");
                log.info("  - Nombre: {}", fileMetadata.getName());
                log.info("  - Tipo MIME: {}", fileMetadata.getMimeType());
                log.info("  - Tamaño: {} bytes", fileMetadata.getSize());
            } catch (Exception e) {
                log.warn("No se pudo obtener metadatos del archivo: {}", e.getMessage());
            }

            // Descargar el contenido del archivo
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);

            byte[] contenido = outputStream.toByteArray();
            log.info("Archivo descargado exitosamente: {} bytes ({}KB)",
                     contenido.length,
                     String.format("%.2f", contenido.length / 1024.0));

            return contenido;

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            // Error de la API de Google
            log.error("Error de Google Drive API: {} - {}", e.getStatusCode(), e.getDetails().getMessage());
            
            if (e.getStatusCode() == 404) {
                throw new IOException("Archivo no encontrado en Google Drive (ID: " + fileId + ")", e);
            } else if (e.getStatusCode() == 403) {
                throw new IOException(
                    "No tienes permiso para acceder a este archivo. " +
                    "Asegúrate de haber compartido el archivo con la Service Account: " +
                    getServiceAccountEmail(), e);
            } else {
                throw new IOException("Error al acceder a Google Drive: " + e.getDetails().getMessage(), e);
            }

        } catch (IOException e) {
            log.error("Error de conexión al descargar desde Google Drive", e);
            throw new IOException("Error de conexión con Google Drive: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Error inesperado al descargar desde Google Drive", e);
            throw new Exception("Error inesperado al acceder a Google Drive: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el email de la Service Account (para compartir archivos)
     */
    public String getServiceAccountEmail() {
        try {
            if (credentialsPath == null || credentialsPath.isEmpty()) {
                return "No configurada";
            }

            try (InputStream in = new FileInputStream(credentialsPath)) {
                GoogleCredentials credentials = ServiceAccountCredentials.fromStream(in);
                if (credentials instanceof ServiceAccountCredentials) {
                    return ((ServiceAccountCredentials) credentials).getServiceAccountUser();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el email de la Service Account", e);
        }
        return "Desconocida";
    }

    /**
     * Verifica si el servicio está configurado correctamente
     */
    public boolean isConfigured() {
        return credentialsPath != null && 
               !credentialsPath.isEmpty() && 
               Files.exists(Paths.get(credentialsPath));
    }
}
