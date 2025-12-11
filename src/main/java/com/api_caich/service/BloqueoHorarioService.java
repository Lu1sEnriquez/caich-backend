package com.api_caich.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.api_caich.dto.bloqueo.CrearBloqueoHorarioDTO;
import com.api_caich.entity.BloqueoHorario;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.Usuario;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.BloqueoHorarioRepository;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.UsuarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueoHorarioService {

    private final BloqueoHorarioRepository bloqueoHorarioRepository;
    private final EspacioRepository espacioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public List<BloqueoHorario> obtenerTodosBloqueos() {
        return bloqueoHorarioRepository.findAll();
    }

    public List<BloqueoHorario> obtenerBloqueosPorEspacio(Long espacioId) {
        return bloqueoHorarioRepository.findByEspacioEspacioId(espacioId);
    }

    public List<BloqueoHorario> obtenerBloqueosGlobales() {
        return bloqueoHorarioRepository.findByEspacioIsNull();
    }

    @Transactional
    public BloqueoHorario crearBloqueo(CrearBloqueoHorarioDTO crearBloqueoDTO, Long usuarioId) {
        // Validaciones
        if (crearBloqueoDTO.getFechaFin().isBefore(crearBloqueoDTO.getFechaInicio())) {
            throw new BusinessException("La fecha fin debe ser posterior a la fecha inicio");
        }

        if (crearBloqueoDTO.getFechaInicio().isBefore(LocalDate.now())) {
            throw new BusinessException("No se pueden crear bloqueos para fechas pasadas");
        }

        BloqueoHorario bloqueo = new BloqueoHorario();
        
        // Si se especifica espacio, validar que exista
        if (crearBloqueoDTO.getEspacioId() != null) {
            Espacio espacio = espacioRepository.findById(crearBloqueoDTO.getEspacioId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));
            bloqueo.setEspacio(espacio);
        }

        Usuario creadoPor = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        bloqueo.setFechaInicio(crearBloqueoDTO.getFechaInicio().atStartOfDay());
        bloqueo.setFechaFin(crearBloqueoDTO.getFechaFin().atTime(23, 59, 59));
        bloqueo.setMotivo(crearBloqueoDTO.getMotivo());
        bloqueo.setTodoElDia(crearBloqueoDTO.getTodoElDia());
        bloqueo.setCreadoPor(creadoPor);

        BloqueoHorario savedBloqueo = bloqueoHorarioRepository.save(bloqueo);

        log.info("Bloqueo creado: {} - {} a {}", 
            bloqueo.getEspacio() != null ? bloqueo.getEspacio().getNombre() : "GLOBAL",
            bloqueo.getFechaInicio(), bloqueo.getFechaFin());

        // Auditoría
        auditoriaService.registrarCambio("BloqueoHorario", savedBloqueo.getBloqueoId(), "Crear",
            "Bloqueo creado", null, null, usuarioId, "SYSTEM");

        return savedBloqueo;
    }

    @Transactional
    public void eliminarBloqueo(Long bloqueoId, Long usuarioId) {
        BloqueoHorario bloqueo = bloqueoHorarioRepository.findById(bloqueoId)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));

        bloqueoHorarioRepository.delete(bloqueo);

        log.info("Bloqueo eliminado: {}", bloqueoId);

        // Auditoría
        auditoriaService.registrarCambio("BloqueoHorario", bloqueoId, "Eliminar",
            "Bloqueo eliminado", null, null, usuarioId, "SYSTEM");
    }

    public boolean verificarDisponibilidad(Long espacioId, LocalDateTime fechaHora) {
        return !bloqueoHorarioRepository.isHorarioBloqueado(
            espacioId, 
            fechaHora.toLocalDate(), 
            fechaHora.toLocalTime(),
            false
        );
    }
}
