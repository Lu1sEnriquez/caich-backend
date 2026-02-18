package com.api_caich.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.espacio.ActualizarEspacioDTO;
import com.api_caich.dto.espacio.CrearEspacioDTO;
import com.api_caich.dto.espacio.EspacioDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final AuditoriaService auditoriaService;

    public List<EspacioDTO> obtenerTodosEspacios() {
        List<Espacio> espacios = espacioRepository.findAll();
        return espacios.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<EspacioDTO> obtenerEspaciosActivos() {
        List<Espacio> espacios = espacioRepository.findByEstaActivoTrue();
        return espacios.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<EspacioDTO> obtenerEspaciosPorTipo(String tipo) {
        List<Espacio> espacios = espacioRepository.findByTipo(tipo);
        return espacios.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<EspacioDTO> obtenerEspaciosDisponiblesPorCapacidad(Integer capacidadMinima) {
        List<Espacio> espacios = espacioRepository.findEspaciosDisponiblesPorCapacidad(capacidadMinima);
        return espacios.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public EspacioDTO obtenerEspacioPorId(Long espacioId) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado con ID: " + espacioId));
        return mapToDTO(espacio);
    }

    @Transactional
    public EspacioDTO crearEspacio(CrearEspacioDTO crearEspacioDTO, Long usuarioId) {
        // Validar que no exista un espacio con el mismo nombre
        if (espacioRepository.existsByNombre(crearEspacioDTO.getNombre())) {
            throw new BusinessException("Ya existe un espacio con el nombre: " + crearEspacioDTO.getNombre());
        }

        // Validar tipo de espacio
        validarTipoEspacio(crearEspacioDTO.getTipo());

        // Crear espacio
        Espacio espacio = new Espacio();
        espacio.setNombre(crearEspacioDTO.getNombre());
        espacio.setTipo(crearEspacioDTO.getTipo());
        espacio.setDescripcion(crearEspacioDTO.getDescripcion());
        espacio.setCapacidad(crearEspacioDTO.getCapacidad());
        espacio.setCostoPorHora(crearEspacioDTO.getCostoPorHora());
        espacio.setEstaActivo(crearEspacioDTO.getEstaActivo());

        Espacio savedEspacio = espacioRepository.save(espacio);

        log.info("Espacio creado: {} - {}", savedEspacio.getNombre(), savedEspacio.getTipo());

        // Auditoría
        auditoriaService.registrarCambio("Espacios", savedEspacio.getEspacioId(), "Crear",
                "Todos los campos", null, null, usuarioId, "SYSTEM");

        return mapToDTO(savedEspacio);
    }

    @Transactional
    public EspacioDTO actualizarEspacio(Long espacioId, ActualizarEspacioDTO actualizarEspacioDTO, Long usuarioId) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado con ID: " + espacioId));

        String cambios = "";

        // Validar y actualizar nombre si es diferente
        if (actualizarEspacioDTO.getNombre() != null
                && !actualizarEspacioDTO.getNombre().equals(espacio.getNombre())) {

            if (espacioRepository.existsByNombreAndEspacioIdNot(actualizarEspacioDTO.getNombre(), espacioId)) {
                throw new BusinessException("Ya existe un espacio con el nombre: " + actualizarEspacioDTO.getNombre());
            }
            cambios += "Nombre: " + espacio.getNombre() + " -> " + actualizarEspacioDTO.getNombre() + "; ";
            espacio.setNombre(actualizarEspacioDTO.getNombre());
        }

        // Actualizar otros campos
        if (actualizarEspacioDTO.getTipo() != null) {
            validarTipoEspacio(actualizarEspacioDTO.getTipo());
            cambios += "Tipo: " + espacio.getTipo() + " -> " + actualizarEspacioDTO.getTipo() + "; ";
            espacio.setTipo(actualizarEspacioDTO.getTipo());
        }

        if (actualizarEspacioDTO.getDescripcion() != null) {
            cambios += "Descripcion actualizada; ";
            espacio.setDescripcion(actualizarEspacioDTO.getDescripcion());
        }

        if (actualizarEspacioDTO.getCapacidad() != null) {
            cambios += "Capacidad: " + espacio.getCapacidad() + " -> " + actualizarEspacioDTO.getCapacidad() + "; ";
            espacio.setCapacidad(actualizarEspacioDTO.getCapacidad());
        }

        if (actualizarEspacioDTO.getCostoPorHora() != null) {
            cambios += "CostoPorHora: " + espacio.getCostoPorHora() + " -> " + actualizarEspacioDTO.getCostoPorHora()
                    + "; ";
            espacio.setCostoPorHora(actualizarEspacioDTO.getCostoPorHora());
        }

        if (actualizarEspacioDTO.getEstaActivo() != null) {
            cambios += "EstaActivo: " + espacio.getEstaActivo() + " -> " + actualizarEspacioDTO.getEstaActivo() + "; ";
            espacio.setEstaActivo(actualizarEspacioDTO.getEstaActivo());
        }

        Espacio updatedEspacio = espacioRepository.save(espacio);

        log.info("Espacio actualizado: {} - Cambios: {}", updatedEspacio.getNombre(), cambios);

        // Auditoría si hubo cambios
        if (!cambios.isEmpty()) {
            auditoriaService.registrarCambio("Espacios", espacioId, "Actualizar",
                    cambios, null, null, usuarioId, "SYSTEM");
        }

        return mapToDTO(updatedEspacio);
    }

    @Transactional
    public EspacioDTO cambiarEstadoEspacio(Long espacioId, Boolean estaActivo, Long usuarioId) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado con ID: " + espacioId));

        Boolean estadoAnterior = espacio.getEstaActivo();
        espacio.setEstaActivo(estaActivo);

        Espacio updatedEspacio = espacioRepository.save(espacio);

        log.info("Estado de espacio cambiado: {} - {} -> {}",
                espacio.getNombre(), estadoAnterior, estaActivo);

        // Auditoría
        String cambios = "EstaActivo: " + estadoAnterior + " -> " + estaActivo;
        auditoriaService.registrarCambio("Espacios", espacioId, "Actualizar",
                cambios, null, null, usuarioId, "SYSTEM");

        return mapToDTO(updatedEspacio);
    }

    @Transactional
    public void eliminarEspacio(Long espacioId, Long usuarioId) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado con ID: " + espacioId));

        // Verificar si el espacio tiene tickets relacionados
        // if (!espacio.getTickets().isEmpty()) {
        //     throw new BusinessException("No se puede eliminar el espacio porque tiene tickets relacionados");
        // }
        espacioRepository.delete(espacio);

        log.info("Espacio eliminado: {} - {}", espacio.getNombre(), espacio.getTipo());

        // Auditoría
        auditoriaService.registrarCambio("Espacios", espacioId, "Eliminar",
                "Espacio eliminado", null, null, usuarioId, "SYSTEM");
    }

    public Long contarEspaciosActivos() {
        return espacioRepository.countByEstaActivoTrue();
    }

    public List<String> obtenerTiposEspacio() {
        return List.of("Cubículo", "Consultorio", "Sala", "Online");
    }

    // Métodos de validación
    private void validarTipoEspacio(String tipo) {
        List<String> tiposValidos = List.of("Cubículo", "Consultorio", "Sala", "Online");
        if (!tiposValidos.contains(tipo)) {
            throw new BusinessException("Tipo de espacio no válido: " + tipo
                    + ". Tipos válidos: " + String.join(", ", tiposValidos));
        }
    }

    // Mapper
    private EspacioDTO mapToDTO(Espacio espacio) {
        return EspacioDTO.builder()
                .espacioId(espacio.getEspacioId())
                .nombre(espacio.getNombre())
                .tipo(espacio.getTipo())
                .descripcion(espacio.getDescripcion())
                .capacidad(espacio.getCapacidad())
                .costoPorHora(espacio.getCostoPorHora())
                .estaActivo(espacio.getEstaActivo())
                .build();
    }
}
