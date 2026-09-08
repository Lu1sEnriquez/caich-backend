package com.api_caich.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para acceso a archivos remotos (principalmente Google Drive)
 * Actúa como proxy para descargas que tienen restricciones de CORS
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileProxyController {

    private final RestTemplate restTemplate;

    /**
     * Proxy para descargar archivos desde Google Drive
     * 
     * Uso: /caich-api/files/drive-proxy?id=1Cc7Yw7guYgnpWkgXi9UzF-O4_woLDg2b
     * 
     * @param fileId ID del archivo en Google Drive
     * @return Archivo descargado con headers CORS
     */
    @GetMapping("/drive-proxy")
    public ResponseEntity<Resource> proxyDriveDownload(@RequestParam String fileId) {
        log.info("Descargando archivo desde Google Drive: {}", fileId);

        if (fileId == null || fileId.trim().isEmpty()) {
            log.warn("ID de archivo vacío");
            return ResponseEntity.badRequest().build();
        }

        try {
            // Generar URL de descarga con parámetro confirm=t para evitar confirmación
            String downloadUrl = String.format(
                    "https://drive.google.com/uc?export=download&id=%s&confirm=t",
                    fileId.trim()
            );

            log.debug("URL de descarga: {}", downloadUrl);

            // Configurar headers para la solicitud
            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(
                    new org.springframework.http.HttpHeaders() {{
                        set("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
                        set("Accept", "image/*,application/*");
                    }}
            );

            // Descargar el archivo
            ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Error descargando archivo. Status: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).build();
            }

            byte[] fileContent = response.getBody();
            log.info("Archivo descargado: {} bytes", fileContent.length);

            // Crear recurso
            Resource resource = new ByteArrayResource(fileContent);

            // Determinar tipo de contenido
            String contentType = determineContentType(fileContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileContent.length);
            headers.set("Content-Disposition", "inline; filename=comprobante");
            headers.set("Cache-Control", "public, max-age=3600");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (RestClientException e) {
            log.error("Error de conexión al descargar desde Google Drive: {}", e.getMessage());
            return ResponseEntity.status(503).build(); // Service Unavailable

        } catch (Exception e) {
            log.error("Error inesperado al descargar archivo de Google Drive", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Determina el tipo de contenido basándose en los magic bytes
     */
    private String determineContentType(byte[] content) {
        if (content == null || content.length < 4) {
            return "application/octet-stream";
        }

        // JPEG: FF D8 FF
        if (content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47
        if (content[0] == (byte) 0x89 && 
            content[1] == (byte) 0x50 && 
            content[2] == (byte) 0x4E && 
            content[3] == (byte) 0x47) {
            return "image/png";
        }

        // GIF: 47 49 46 38
        if (content[0] == (byte) 0x47 && 
            content[1] == (byte) 0x49 && 
            content[2] == (byte) 0x46 && 
            content[3] == (byte) 0x38) {
            return "image/gif";
        }

        // PDF: 25 50 44 46
        if (content[0] == (byte) 0x25 && 
            content[1] == (byte) 0x50 && 
            content[2] == (byte) 0x44 && 
            content[3] == (byte) 0x46) {
            return "application/pdf";
        }

        // WebP: 52 49 46 46 ... 57 45 42 50
        if (content.length >= 12 &&
            content[0] == (byte) 0x52 && 
            content[1] == (byte) 0x49 && 
            content[2] == (byte) 0x46 && 
            content[3] == (byte) 0x46) {
            return "image/webp";
        }

        return "application/octet-stream";
    }
}
