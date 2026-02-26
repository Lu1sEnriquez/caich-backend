package com.api_caich.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.horario.ExcepcionHorarioDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.ExcepcionHorario;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ExcepcionHorarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio CRUD para gestionar ExcepcionHorario.
 * Maneja la creación, actualización y eliminación de excepciones horarias
 * (bloqueos, festivos, mantenimientos, etc).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcepcionHorarioService {

    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final EspacioRepository espacioRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Obtiene una excepción específica.
     */
    public ExcepcionHorarioDTO obtener(Long excepcionId) {
        ExcepcionHorario excepcion = excepcionHorarioRepository.findById(excepcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Excepción horaria no encontrada"));

        return mapToDTO(excepcion);
    }

    /**
     * Obtiene todas las excepciones para un espacio en una rango de fechas.
     * Incluye excepciones específicas del espacio + excepciones globales.
     */
    public List<ExcepcionHorarioDTO> obtenerPorEspacioEnRango(Long espacioId, LocalDate fechaInicio, LocalDate fechaFin) {
        // Excepciones específicas del espacio
        List<ExcepcionHorario> exceptacionesEspecificas = excepcionHorarioRepository
            .findByEspacioAndFechaBetween(espacioId, fechaInicio, fechaFin);
        
        // Excepciones globales en el mismo rango
        List<ExcepcionHorario> excepcionesGlobales = excepcionHorarioRepository
            .findExcepcionesGlobalesPorFechaBetween(fechaInicio, fechaFin);
        
        // Combinar ambas listas
        List<ExcepcionHorario> todasLasExcepciones = new ArrayList<>();
        todasLasExcepciones.addAll(exceptacionesEspecificas);
        todasLasExcepciones.addAll(excepcionesGlobales);

        return todasLasExcepciones.stream().map(this::mapToDTO).toList();
    }

    /**
     * Obtiene todas las excepciones globales en un rango de fechas.
     */
    public List<ExcepcionHorarioDTO> obtenerExcepcionesGlobalesEnRango(LocalDate fechaInicio, LocalDate fechaFin) {
        List<ExcepcionHorario> excepcionesGlobales = excepcionHorarioRepository
            .findExcepcionesGlobalesPorFechaBetween(fechaInicio, fechaFin);

        return excepcionesGlobales.stream().map(this::mapToDTO).toList();
    }

    /**
     * Obtiene todas las excepciones activas.
     */
    public List<ExcepcionHorarioDTO> obtenerTodosActivos() {
        List<ExcepcionHorario> excepciones = excepcionHorarioRepository.findByEstaActivoTrue();
        return excepciones.stream().map(this::mapToDTO).toList();
    }

    /**
     * Crea una nueva excepción horaria.
     */
    @Transactional
    public ExcepcionHorarioDTO crear(ExcepcionHorarioDTO dto, Long usuarioId) {
        // Validaciones
        if (dto.getFecha() != null && dto.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("No se pueden crear excepciones para fechas pasadas");
        }

        // Si no es todo el día, validar horarios
        if (dto.getTodoElDia() != null && !dto.getTodoElDia()) {
            if (dto.getHoraInicioBloqueo() == null || dto.getHoraFinBloqueo() == null) {
                throw new BusinessException("Si no es todo el día, debe especificar hora inicio y fin");
            }

            if (dto.getHoraInicioBloqueo().isAfter(dto.getHoraFinBloqueo())) {
                throw new BusinessException("La hora de inicio debe ser anterior a la hora de fin");
            }
        }

        Espacio espacio = null;
        if (dto.getEspacioId() != null) {
            espacio = espacioRepository.findById(dto.getEspacioId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));
        }

        ExcepcionHorario excepcion = ExcepcionHorario.builder()
            .espacio(espacio)
            .fecha(dto.getFecha())
            .todoElDia(dto.getTodoElDia() != null ? dto.getTodoElDia() : false)
            .horaInicioBloqueo(dto.getHoraInicioBloqueo())
            .horaFinBloqueo(dto.getHoraFinBloqueo())
            .motivo(dto.getMotivo())
            .estaActivo(true)
            .build();

        ExcepcionHorario saved = excepcionHorarioRepository.save(excepcion);

        log.info("Excepción horaria creada: {} - {} ({})", 
            dto.getFecha(), 
            dto.getTodoElDia() ? "Todo el día" : "Parcial",
            dto.getMotivo());

        auditoriaService.registrarCambio("ExcepcionHorario", saved.getExcepcionHorarioId(), "Crear",
            String.format("Excepción: %s (%s)", dto.getFecha(), dto.getMotivo()),
            null, null, usuarioId, "SYSTEM");

        return mapToDTO(saved);
    }

    /**
     * Actualiza una excepción horaria.
     */
    @Transactional
    public ExcepcionHorarioDTO actualizar(Long excepcionId, ExcepcionHorarioDTO dto, Long usuarioId) {
        ExcepcionHorario excepcion = excepcionHorarioRepository.findById(excepcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Excepción horaria no encontrada"));

        // Validaciones
        if (dto.getFecha() != null) {
            if (dto.getFecha().isBefore(LocalDate.now()) && !excepcion.getFecha().equals(dto.getFecha())) {
                throw new BusinessException("No se pueden cambiar a fecha pasada");
            }
            excepcion.setFecha(dto.getFecha());
        }

        if (dto.getTodoElDia() != null) {
            excepcion.setTodoElDia(dto.getTodoElDia());
        }

        if (dto.getHoraInicioBloqueo() != null) {
            excepcion.setHoraInicioBloqueo(dto.getHoraInicioBloqueo());
        }

        if (dto.getHoraFinBloqueo() != null) {
            excepcion.setHoraFinBloqueo(dto.getHoraFinBloqueo());
        }

        if (dto.getMotivo() != null) {
            excepcion.setMotivo(dto.getMotivo());
        }

        if (dto.getEstaActivo() != null) {
            excepcion.setEstaActivo(dto.getEstaActivo());
        }

        ExcepcionHorario saved = excepcionHorarioRepository.save(excepcion);

        log.info("Excepción horaria actualizada: {}", excepcionId);

        auditoriaService.registrarCambio("ExcepcionHorario", excepcionId, "Actualizar",
            String.format("Excepción: %s actualizada", dto.getFecha()),
            null, null, usuarioId, "SYSTEM");

        return mapToDTO(saved);
    }

    /**
     * Elimina lógicamente una excepción (la desactiva).
     */
    @Transactional
    public void eliminar(Long excepcionId, Long usuarioId) {
        ExcepcionHorario excepcion = excepcionHorarioRepository.findById(excepcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Excepción horaria no encontrada"));

        excepcion.setEstaActivo(false);
        excepcionHorarioRepository.save(excepcion);

        log.info("Excepción horaria eliminada: {}", excepcionId);

        auditoriaService.registrarCambio("ExcepcionHorario", excepcionId, "Eliminar",
            "Excepción desactivada", null, null, usuarioId, "SYSTEM");
    }

    // Mapper
    private ExcepcionHorarioDTO mapToDTO(ExcepcionHorario excepcion) {
        return ExcepcionHorarioDTO.builder()
            .excepcionHorarioId(excepcion.getExcepcionHorarioId())
            .espacioId(excepcion.getEspacio() != null ? excepcion.getEspacio().getEspacioId() : null)
            .fecha(excepcion.getFecha())
            .todoElDia(excepcion.getTodoElDia())
            .horaInicioBloqueo(excepcion.getHoraInicioBloqueo())
            .horaFinBloqueo(excepcion.getHoraFinBloqueo())
            .motivo(excepcion.getMotivo())
            .estaActivo(excepcion.getEstaActivo())
            .build();
    }
}
