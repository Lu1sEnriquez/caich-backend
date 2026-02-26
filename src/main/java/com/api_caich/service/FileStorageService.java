package com.api_caich.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:5242880}") // 5MB
    private long maxFileSize;

    private static final String[] ALLOWED_EXTENSIONS = { "jpg", "jpeg", "png", "gif" };
    private static final String[] ALLOWED_CONTENT_TYPES = { "image/jpeg", "image/png", "image/gif" };
    
    private static final String[] ALLOWED_COMPROBANTE_EXTENSIONS = { "jpg", "jpeg", "png", "gif", "pdf" };
    private static final String[] ALLOWED_COMPROBANTE_CONTENT_TYPES = { "image/jpeg", "image/png", "image/gif", "application/pdf" };

    public String almacenarFotoPerfil(MultipartFile file, Long usuarioId) {
        // Validaciones básicas
        validarArchivo(file);

        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(uploadDir, "profiles");
            Files.createDirectories(uploadPath);

            // Generar nombre único
            String extension = obtenerExtension(file.getOriginalFilename());
            String fileName = "profile_" + usuarioId + "_" + UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(fileName);

            // Procesar y redimensionar imagen
            BufferedImage imagenOriginal = ImageIO.read(file.getInputStream());
            BufferedImage imagenRedimensionada = redimensionarImagen(imagenOriginal, 300, 300);

            // Guardar imagen procesada
            ImageIO.write(imagenRedimensionada, extension, filePath.toFile());

            log.info("Foto de perfil guardada: {}", filePath.toString());
            //TODO: revisar la ruta devuelta
            // return "/uploads/profiles/" + fileName;
            return fileName;

        } catch (IOException e) {
            log.error("Error al guardar foto de perfil", e);
            throw new BusinessException("Error al guardar la imagen: " + e.getMessage());
        }
    }

    private void validarArchivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(
                    "El archivo es demasiado grande. Tamaño máximo: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        boolean contentTypeValido = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                contentTypeValido = true;
                break;
            }
        }

        if (!contentTypeValido) {
            throw new BusinessException("Tipo de archivo no permitido. Formatos aceptados: JPG, JPEG, PNG, GIF");
        }

        String extension = obtenerExtension(file.getOriginalFilename());
        boolean extensionValida = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                extensionValida = true;
                break;
            }
        }

        if (!extensionValida) {
            throw new BusinessException("Extensión de archivo no permitida. Extensiones aceptadas: " +
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private BufferedImage redimensionarImagen(BufferedImage imagenOriginal, int ancho, int alto) {
        BufferedImage imagenRedimensionada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagenRedimensionada.createGraphics();

        // Configurar calidad de redimensionado
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(imagenOriginal, 0, 0, ancho, alto, null);
        g.dispose();

        return imagenRedimensionada;
    }

    private String obtenerExtension(String fileName) {
        if (fileName == null) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "jpg";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    public boolean eliminarFotoAnterior(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isEmpty()) {
            return true;
        }

        try {
            Path fotoPath = Paths.get("." + fotoUrl); // Agregar . para ruta relativa
            return Files.deleteIfExists(fotoPath);
        } catch (IOException e) {
            log.warn("No se pudo eliminar la foto anterior: {}", fotoUrl, e);
            return false;
        }
    }

    public String almacenarComprobante(MultipartFile file, Long ticketId) {
        // Validaciones básicas (permite imágenes y PDF)
        validarComprobanteArchivo(file);

        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(uploadDir, "comprobantes");
            Files.createDirectories(uploadPath);

            // Generar nombre único
            String extension = obtenerExtension(file.getOriginalFilename());
            String fileName = "comprobante_" + ticketId + "_" + UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(fileName);

            // Guardar archivo (sin procesar imagen para comprobantes)
            Files.copy(file.getInputStream(), filePath);

            log.info("Comprobante guardado: {}", filePath.toString());
            return "/uploads/comprobantes/" + fileName;

        } catch (IOException e) {
            log.error("Error al guardar comprobante", e);
            throw new BusinessException("Error al guardar el comprobante: " + e.getMessage());
        }
    }

    public Resource cargarArchivo(String subdirectorio, String nombreArchivo) {
        try {
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetDir = baseDir.resolve(subdirectorio).normalize();
            Path filePath = targetDir.resolve(nombreArchivo).normalize();

            if (!filePath.startsWith(targetDir)) {
                throw new BusinessException("Ruta de archivo no válida");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Archivo no encontrado");
            }

            return resource;
        } catch (MalformedURLException e) {
            log.error("Error al cargar archivo", e);
            throw new BusinessException("Error al cargar el archivo");
        }
    }

    /**
     * Guarda un comprobante desde bytes (usado para descarga desde Google Drive)
     * @param fileBytes Contenido del archivo en bytes
     * @param ticketId ID del ticket
     * @param extension Extensión del archivo (jpg, png, pdf)
     * @return Ruta relativa del archivo guardado
     */
    public String almacenarComprobanteDesdeBytes(byte[] fileBytes, Long ticketId, String extension) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("El contenido del archivo está vacío");
        }

        if (fileBytes.length > maxFileSize) {
            throw new BusinessException(
                    "El archivo es demasiado grande. Tamaño máximo: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Validar extensión
        boolean extensionValida = false;
        for (String allowedExt : ALLOWED_COMPROBANTE_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                extensionValida = true;
                break;
            }
        }

        if (!extensionValida) {
            throw new BusinessException("Extensión de archivo no permitida. Extensiones aceptadas: " +
                    String.join(", ", ALLOWED_COMPROBANTE_EXTENSIONS));
        }

        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(uploadDir, "comprobantes");
            Files.createDirectories(uploadPath);

            // Generar nombre único
            String fileName = "comprobante_" + ticketId + "_" + UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(fileName);

            // Guardar archivo
            Files.write(filePath, fileBytes);

            log.info("Comprobante desde bytes guardado: {} ({} bytes)", filePath.toString(), fileBytes.length);
            return "/uploads/comprobantes/" + fileName;

        } catch (IOException e) {
            log.error("Error al guardar comprobante desde bytes", e);
            throw new BusinessException("Error al guardar el comprobante: " + e.getMessage());
        }
    }

    private void validarComprobanteArchivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(
                    "El archivo es demasiado grande. Tamaño máximo: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        boolean contentTypeValido = false;
        for (String allowedType : ALLOWED_COMPROBANTE_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                contentTypeValido = true;
                break;
            }
        }

        if (!contentTypeValido) {
            throw new BusinessException("Tipo de archivo no permitido. Formatos aceptados: JPG, JPEG, PNG, GIF, PDF");
        }

        String extension = obtenerExtension(file.getOriginalFilename());
        boolean extensionValida = false;
        for (String allowedExt : ALLOWED_COMPROBANTE_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                extensionValida = true;
                break;
            }
        }

        if (!extensionValida) {
            throw new BusinessException("Extensión de archivo no permitida. Extensiones aceptadas: " +
                    String.join(", ", ALLOWED_COMPROBANTE_EXTENSIONS));
        }
    }
}
