package com.api_caich.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.api_caich.entity.Configuracion;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.ConfiguracionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public Configuracion obtenerActivaPorClave(String clave) {
        return configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new BusinessException("No se encontro configuracion activa para la clave: " + clave));
    }

    @Transactional
    public Configuracion saveOrUpdate(String clave, String valor, String descripcion) {
        // Verificar si ya existe una version activa con el mismo valor
        var configActual = configuracionRepository.findByClave(clave);
        
        if (configActual.isPresent() && configActual.get().getValor().equals(valor)) {
            // El valor no cambió, retornar la configuración actual
            return configActual.get();
        }

        // Solo desactivar la anterior si el valor realmente cambió
        configActual.ifPresent((config) -> {
            config.setActivo(false);
            config.setUltimaActualizacion(LocalDateTime.now());
            configuracionRepository.save(config);
        });

        Configuracion nueva = new Configuracion();
        nueva.setClave(clave);
        nueva.setValor(valor);
        nueva.setDescripcion(descripcion);
        nueva.setActivo(true);
        nueva.setUltimaActualizacion(LocalDateTime.now());

        return configuracionRepository.save(nueva);
    }
}
