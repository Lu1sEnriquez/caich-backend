package com.api_caich.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.horario.HorarioBaseDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.HorarioBase;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.HorarioBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio CRUD para gestionar HorarioBase.
 * Maneja la creación, actualización y eliminación de horarios base.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HorarioBaseService {

    private final HorarioBaseRepository horarioBaseRepository;
    private final EspacioRepository espacioRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Obtiene el horario base para un espacio específico.
     */
    public HorarioBaseDTO obtenerHorarioBase(Long espacioId) {
        HorarioBase horarioBase = horarioBaseRepository.findByEspacioEspacioId(espacioId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe horario base para el espacio " + espacioId));

        return mapToDTO(horarioBase);
    }

    /**
     * Obtiene el horario base global.
     */
    public HorarioBaseDTO obtenerHorarioBaseGlobal() {
        HorarioBase horarioBase = horarioBaseRepository.findHorarioBaseGlobal()
            .orElseThrow(() -> new ResourceNotFoundException("No existe horario base global"));

        return mapToDTO(horarioBase);
    }

    /**
     * Obtiene todos los horarios base activos.
     */
    public List<HorarioBaseDTO> obtenerTodosActivos() {
        List<HorarioBase> horarios = horarioBaseRepository.findByEstaActivoTrue();
        return horarios.stream().map(this::mapToDTO).toList();
    }

    /**
     * Crea o actualiza el horario base para un espacio.
     */
    @Transactional
    public HorarioBaseDTO crearOActualizar(Long espacioId, HorarioBaseDTO dto, Long usuarioId) {
        // Validar que horaApertura < horaCierre
        if (dto.getHoraApertura().isAfter(dto.getHoraCierre())) {
            throw new BusinessException("La hora de apertura debe ser anterior a la hora de cierre");
        }

        // Validar que duracionMínima <= duracionMáxima
        if (dto.getDuracionMinimaMinutos() > dto.getDuracionMaximaMinutos()) {
            throw new BusinessException("La duración mínima debe ser menor o igual a la duración máxima");
        }

        // Verificar si ya existe un horario para este espacio
        HorarioBase horarioExistente = horarioBaseRepository.findByEspacioEspacioId(espacioId)
            .orElse(null);

        HorarioBase horarioBase;
        String operacion;

        if (horarioExistente != null) {
            horarioBase = horarioExistente;
            operacion = "Actualizar";
        } else {
            Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

            horarioBase = new HorarioBase();
            horarioBase.setEspacio(espacio);
            operacion = "Crear";
        }

        horarioBase.setHoraApertura(dto.getHoraApertura());
        horarioBase.setHoraCierre(dto.getHoraCierre());
        horarioBase.setDiasOperativos(dto.getDiasOperativos());
        horarioBase.setIntervaloMinutos(dto.getIntervaloMinutos());
        horarioBase.setDuracionMinimaMinutos(dto.getDuracionMinimaMinutos());
        horarioBase.setDuracionMaximaMinutos(dto.getDuracionMaximaMinutos());
        horarioBase.setEstaActivo(dto.getEstaActivo() != null ? dto.getEstaActivo() : true);

        HorarioBase saved = horarioBaseRepository.save(horarioBase);

        log.info("HorarioBase {} para espacio {}: {}-{}", 
            operacion, espacioId, dto.getHoraApertura(), dto.getHoraCierre());

        // Auditoría
        auditoriaService.registrarCambio("HorarioBase", saved.getHorarioBaseId(), operacion,
            String.format("Horario: %s-%s, Intervalos: %d min, DiasOperativos: %s", 
                dto.getHoraApertura(), dto.getHoraCierre(), 
                dto.getIntervaloMinutos(), dto.getDiasOperativos()),
            null, null, usuarioId, "SYSTEM");

        return mapToDTO(saved);
    }

    /**
     * Elimina el horario base de un espacio.
     * Después de esto, el espacio usará la configuración global como fallback.
     */
    @Transactional
    public void desactivar(Long espacioId, Long usuarioId) {
        HorarioBase horarioBase = horarioBaseRepository.findByEspacioEspacioId(espacioId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe horario base para el espacio " + espacioId));

        Long horarioBaseId = horarioBase.getHorarioBaseId();
        horarioBaseRepository.delete(horarioBase);

        log.info("HorarioBase eliminado para espacio {}. Usará configuración global.", espacioId);

        auditoriaService.registrarCambio("HorarioBase", horarioBaseId, "Eliminar",
            "Horario específico eliminado. Espacio usará configuración global.", null, null, usuarioId, "SYSTEM");
    }

    /**
     * Crea o actualiza el horario base global (UPSERT).
     * Si existe, lo actualiza. Si no existe, lo crea.
     */
    @Transactional
    public HorarioBaseDTO crearOActualizarGlobal(HorarioBaseDTO dto, Long usuarioId) {
        // Validaciones
        if (dto.getHoraApertura().isAfter(dto.getHoraCierre())) {
            throw new BusinessException("La hora de apertura debe ser anterior a la hora de cierre");
        }

        // Validar que duracionMínima <= duracionMáxima
        if (dto.getDuracionMinimaMinutos() > dto.getDuracionMaximaMinutos()) {
            throw new BusinessException("La duración mínima debe ser menor o igual a la duración máxima");
        }

        // Verificar si ya existe un horario global
        HorarioBase horarioBase = horarioBaseRepository.findHorarioBaseGlobal().orElse(null);
        String operacion;

        if (horarioBase != null) {
            // ACTUALIZAR
            operacion = "Actualizar";
        } else {
            // CREAR
            horarioBase = HorarioBase.builder()
                .espacio(null) // Global
                .estaActivo(true)
                .build();
            operacion = "Crear";
        }

        // Actualizar campos
        horarioBase.setHoraApertura(dto.getHoraApertura());
        horarioBase.setHoraCierre(dto.getHoraCierre());
        horarioBase.setDiasOperativos(dto.getDiasOperativos());
        horarioBase.setIntervaloMinutos(dto.getIntervaloMinutos());
        horarioBase.setDuracionMinimaMinutos(dto.getDuracionMinimaMinutos());
        horarioBase.setDuracionMaximaMinutos(dto.getDuracionMaximaMinutos());

        HorarioBase saved = horarioBaseRepository.save(horarioBase);

        log.info("HorarioBase GLOBAL {}: {}-{}", operacion, dto.getHoraApertura(), dto.getHoraCierre());

        auditoriaService.registrarCambio("HorarioBase", saved.getHorarioBaseId(), operacion,
            String.format("Horario global %s: %s-%s, Intervalos: %d min, DiasOperativos: %s", 
                operacion.toLowerCase(), dto.getHoraApertura(), dto.getHoraCierre(), 
                dto.getIntervaloMinutos(), dto.getDiasOperativos()),
            null, null, usuarioId, "SYSTEM");

        return mapToDTO(saved);
    }

    /**
     * Crea el horario base global (DEPRECATED: usar crearOActualizarGlobal en su lugar).
     */
    @Transactional
    @Deprecated
    public HorarioBaseDTO crearGlobal(HorarioBaseDTO dto, Long usuarioId) {
        return crearOActualizarGlobal(dto, usuarioId);
    }

    // Mapper
    private HorarioBaseDTO mapToDTO(HorarioBase horarioBase) {
        return HorarioBaseDTO.builder()
            .horarioBaseId(horarioBase.getHorarioBaseId())
            .espacioId(horarioBase.getEspacio() != null ? horarioBase.getEspacio().getEspacioId() : null)
            .horaApertura(horarioBase.getHoraApertura())
            .horaCierre(horarioBase.getHoraCierre())
            .diasOperativos(horarioBase.getDiasOperativos())
            .intervaloMinutos(horarioBase.getIntervaloMinutos())
            .duracionMinimaMinutos(horarioBase.getDuracionMinimaMinutos())
            .duracionMaximaMinutos(horarioBase.getDuracionMaximaMinutos())
            .estaActivo(horarioBase.getEstaActivo())
            .build();
    }
}
