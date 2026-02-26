package com.api_caich.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración web para servir archivos estáticos y negociación de contenido
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("Configurando handlers de recursos estáticos desde: {}", uploadDir);
        
        // Servir archivos desde /uploads/** 
        // Mapea a la carpeta física en el sistema de archivos
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600); // Cache de 1 hora

        // Servir archivos desde /comprobantes/** 
        // Permite acceso directo: http://localhost:8080/uploads/comprobantes/archivo.jpg
        registry.addResourceHandler("/uploads/comprobantes/**")
                .addResourceLocations("file:" + uploadDir + "/comprobantes/")
                .setCachePeriod(3600);

        // Servir archivos desde /assets/** (por si se necesita más adelante)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);

        log.info("Handlers de recursos configurados:");
        log.info("- /uploads/** → file:{}/", uploadDir);
        log.info("- /uploads/comprobantes/** → file:{}/comprobantes/", uploadDir);
        log.info("- /assets/** → file:{}/", uploadDir);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON);
    }
}
