package com.api_caich.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.configuracion.ConfiguracionDTO;
import com.api_caich.dto.configuracion.ConfiguracionRequestDTO;
import com.api_caich.entity.Configuracion;
import com.api_caich.service.ConfiguracionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/configuracion")
@RequiredArgsConstructor
@Tag(name = "Configuracion", description = "Configuraciones operativas con versionado")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener configuracion activa por clave")
    public ResponseEntity<DataResponseDTO<ConfiguracionDTO>> obtenerActiva(
            @PathVariable String clave) {

        Configuracion configuracion = configuracionService.obtenerActivaPorClave(clave);
        ConfiguracionDTO dto = ConfiguracionDTO.fromEntity(configuracion);

        return ResponseEntity.ok(DataResponseDTO.success(dto, "Configuracion obtenida correctamente"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar configuracion con versionado")
    public ResponseEntity<DataResponseDTO<ConfiguracionDTO>> saveOrUpdate(
            @Valid @RequestBody ConfiguracionRequestDTO request) {

        Configuracion configuracion = configuracionService.saveOrUpdate(
                request.getClave(),
                request.getValor(),
                request.getDescripcion());

        ConfiguracionDTO dto = ConfiguracionDTO.fromEntity(configuracion);

        return ResponseEntity.ok(DataResponseDTO.success(dto, "Configuracion guardada como nueva version"));
    }
}
