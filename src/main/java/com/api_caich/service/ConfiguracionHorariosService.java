package com.api_caich.service;

import com.api_caich.dto.horario.ConfiguracionHorariosDTO;
import com.api_caich.dto.horario.CrearConfiguracionHorariosDTO;
import com.api_caich.entity.BloqueoHorario;
import com.api_caich.entity.ConfiguracionEspacioHorario;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.Usuario;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.BloqueoHorarioRepository;
import com.api_caich.repository.ConfiguracionEspacioHorarioRepository;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionHorariosService {

    private final ConfiguracionEspacioHorarioRepository configuracionRepository;
    private final EspacioRepository espacioRepository;
    private final UsuarioRepository usuarioRepository;
    private final BloqueoHorarioRepository bloqueoHorarioRepository;
    private final AuditoriaService auditoriaService;
    private static final LocalTime HORA_INICIO_DEFAULT = LocalTime.of(7, 0);
    private static final LocalTime HORA_FIN_DEFAULT = LocalTime.of(18, 0);

    public ConfiguracionHorariosDTO obtenerConfiguracionPorFecha(Long espacioId, LocalDate fecha) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        // Obtener todas las configuraciones para este espacio y fecha
        List<ConfiguracionEspacioHorario> configuraciones = configuracionRepository
                .findByEspacioEspacioIdAndFecha(espacioId, fecha);

        // Extraer las horas deshabilitadas (donde EstaDisponible = false)
        List<String> horasDeshabilitadas = configuraciones.stream()
                .filter(config -> !config.getEstaDisponible())
                .map(ConfiguracionEspacioHorario::getHoraInicio)
                .map(LocalTime::toString)
                .collect(Collectors.toList());

        return ConfiguracionHorariosDTO.builder()
                .espacioId(espacioId)
                .espacioNombre(espacio.getNombre())
                .fecha(fecha)
                .horasDeshabilitadas(horasDeshabilitadas)
                .build();
    }

    public List<ConfiguracionHorariosDTO> obtenerConfiguracionesPorEspacio(Long espacioId) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        List<ConfiguracionEspacioHorario> configuraciones = configuracionRepository.findByEspacioEspacioId(espacioId);

        // Agrupar por fecha
        return configuraciones.stream()
                .collect(Collectors.groupingBy(ConfiguracionEspacioHorario::getFecha))
                .entrySet().stream()
                .map(entry -> {
                    LocalDate fecha = entry.getKey();
                    List<String> horasDeshabilitadas = entry.getValue().stream()
                            .filter(config -> !config.getEstaDisponible())
                            .map(ConfiguracionEspacioHorario::getHoraInicio)
                            .map(LocalTime::toString)
                            .collect(Collectors.toList());

                    return ConfiguracionHorariosDTO.builder()
                            .espacioId(espacioId)
                            .espacioNombre(espacio.getNombre())
                            .fecha(fecha)
                            .horasDeshabilitadas(horasDeshabilitadas)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ConfiguracionHorariosDTO guardarConfiguracion(CrearConfiguracionHorariosDTO configuracionDTO,
            Long usuarioId) {
        Espacio espacio = espacioRepository.findById(configuracionDTO.getEspacioId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar que la fecha no sea en el pasado
        if (configuracionDTO.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede configurar horarios para fechas pasadas");
        }

        // Eliminar configuraciones existentes para esta fecha y espacio
        configuracionRepository.deleteByEspacioEspacioIdAndFecha(
                configuracionDTO.getEspacioId(), configuracionDTO.getFecha());

        // Crear nuevas configuraciones para cada hora deshabilitada
        List<ConfiguracionEspacioHorario> nuevasConfiguraciones = new ArrayList<>();

        for (String horaStr : configuracionDTO.getHorasDeshabilitadas()) {
            try {
                LocalTime hora = LocalTime.parse(horaStr);

                ConfiguracionEspacioHorario configuracion = new ConfiguracionEspacioHorario();
                configuracion.setEspacio(espacio);
                configuracion.setFecha(configuracionDTO.getFecha());
                configuracion.setHoraInicio(hora);
                configuracion.setHoraFin(hora.plusHours(1)); // Asumir 1 hora de bloqueo
                configuracion.setEstaDisponible(false); // Deshabilitado

                nuevasConfiguraciones.add(configuracion);
            } catch (Exception e) {
                throw new BusinessException("Formato de hora inválido: " + horaStr + ". Use formato HH:mm");
            }
        }

        List<ConfiguracionEspacioHorario> savedConfiguraciones = configuracionRepository.saveAll(nuevasConfiguraciones);

        log.info("Configuración de horarios guardada para espacio: {} fecha: {} - {} horas deshabilitadas",
                espacio.getNombre(), configuracionDTO.getFecha(), configuracionDTO.getHorasDeshabilitadas().size());

        // Auditoría
        if (!savedConfiguraciones.isEmpty()) {
            auditoriaService.registrarCambio("ConfiguracionEspacioHorario",
                    savedConfiguraciones.get(0).getConfigId(), "Crear",
                    "Configuración de horarios guardada", null, null, usuarioId, "SYSTEM");
        }

        return ConfiguracionHorariosDTO.builder()
                .espacioId(configuracionDTO.getEspacioId())
                .espacioNombre(espacio.getNombre())
                .fecha(configuracionDTO.getFecha())
                .horasDeshabilitadas(configuracionDTO.getHorasDeshabilitadas())
                .build();
    }

    @Transactional
    public void eliminarConfiguracion(Long configId) {
        ConfiguracionEspacioHorario configuracion = configuracionRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada"));

        configuracionRepository.delete(configuracion);
        log.info("Configuración de horarios eliminada: {}", configId);
    }

    @Transactional
    public void eliminarConfiguracionesPorEspacioYFecha(Long espacioId, LocalDate fecha) {
        configuracionRepository.deleteByEspacioEspacioIdAndFecha(espacioId, fecha);
        log.info("Configuraciones eliminadas para espacio: {} fecha: {}", espacioId, fecha);
    }

    public boolean verificarDisponibilidad(Long espacioId, LocalDate fecha, String horaInicioStr, String horaFinStr) {
        try {
            LocalTime horaInicio = LocalTime.parse(horaInicioStr);
            LocalTime horaFin = LocalTime.parse(horaFinStr);

            // Verificar si hay configuraciones que bloqueen este horario
            List<ConfiguracionEspacioHorario> configuracionesConflictivas = configuracionRepository
                    .findConflictingHorarios(espacioId, fecha, horaInicio, horaFin);

            return configuracionesConflictivas.isEmpty();
        } catch (Exception e) {
            throw new BusinessException("Error al verificar disponibilidad: " + e.getMessage());
        }
    }

    public List<LocalTime> obtenerHorariosDisponibles(Long espacioId, LocalDate fecha) {
        // 1. Verificar si hay bloqueos globales o para este espacio
        if (tieneBloqueos(espacioId, fecha)) {
            return new ArrayList<>(); // Día completamente bloqueado
        }

        // 2. Obtener configuraciones específicas para este día
        List<ConfiguracionEspacioHorario> configuracionesEspecificas = configuracionRepository
                .findByEspacioEspacioIdAndFecha(espacioId, fecha);

        // 3. Si hay configuraciones específicas, usarlas
        if (!configuracionesEspecificas.isEmpty()) {
            return generarHorariosDesdeConfiguracion(configuracionesEspecificas);
        }

        // 4. Si no hay configuraciones específicas, usar horario por defecto (L-V 7-18)
        return generarHorariosDefault(fecha);
    }

    public boolean tieneBloqueos(Long espacioId, LocalDate fecha) {
        // Usar el método corregido que ya funciona con LocalDate
        List<BloqueoHorario> bloqueos = bloqueoHorarioRepository.findBloqueosActivos(espacioId, fecha);
        return !bloqueos.isEmpty();
    }

    // Método alternativo si prefieres usar el enfoque con LocalDateTime
    public boolean tieneBloqueosAlternativo(Long espacioId, LocalDate fecha) {
        // Convertir LocalDate a LocalDateTime para el rango completo del día
        LocalDateTime fechaInicio = fecha.atStartOfDay(); // 00:00:00
        LocalDateTime fechaFin = fecha.atTime(23, 59, 59); // 23:59:59

        List<BloqueoHorario> bloqueos = bloqueoHorarioRepository.findBloqueosEnRango(
                espacioId, fechaInicio, fechaFin);

        return !bloqueos.isEmpty();
    }

    /**
     * Verificar si un día tiene configuración especial
     */
    public boolean tieneConfiguracionEspecial(Long espacioId, LocalDate fecha) {
        List<ConfiguracionEspacioHorario> configuraciones = configuracionRepository
                .findByEspacioEspacioIdAndFecha(espacioId, fecha);
        return !configuraciones.isEmpty();
    }

    public List<ConfiguracionHorariosDTO> obtenerConfiguracionesPorRangoFechas(LocalDate fechaInicio,
            LocalDate fechaFin) {
        List<ConfiguracionEspacioHorario> configuraciones = configuracionRepository.findByFechaBetween(fechaInicio,
                fechaFin);

        // Agrupar por espacio y fecha
        return configuraciones.stream()
                .collect(Collectors.groupingBy(config -> config.getEspacio().getEspacioId() + "_" + config.getFecha()))
                .values().stream()
                .map(configList -> {
                    if (configList.isEmpty())
                        return null;

                    ConfiguracionEspacioHorario primeraConfig = configList.get(0);
                    List<String> horasDeshabilitadas = configList.stream()
                            .filter(config -> !config.getEstaDisponible())
                            .map(ConfiguracionEspacioHorario::getHoraInicio)
                            .map(LocalTime::toString)
                            .collect(Collectors.toList());

                    return ConfiguracionHorariosDTO.builder()
                            .espacioId(primeraConfig.getEspacio().getEspacioId())
                            .espacioNombre(primeraConfig.getEspacio().getNombre())
                            .fecha(primeraConfig.getFecha())
                            .horasDeshabilitadas(horasDeshabilitadas)
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Generar horarios basados en configuración específica
     */
    private List<LocalTime> generarHorariosDesdeConfiguracion(List<ConfiguracionEspacioHorario> configuraciones) {
        List<LocalTime> horariosDisponibles = new ArrayList<>();

        // Extraer horas deshabilitadas
        Set<LocalTime> horasDeshabilitadas = configuraciones.stream()
                .filter(config -> !config.getEstaDisponible())
                .map(ConfiguracionEspacioHorario::getHoraInicio)
                .collect(Collectors.toSet());

        // Generar horarios de 7:00 a 18:00 (por defecto) excluyendo los deshabilitados
        for (LocalTime hora = HORA_INICIO_DEFAULT; hora.isBefore(HORA_FIN_DEFAULT); hora = hora.plusHours(1)) {

            if (!horasDeshabilitadas.contains(hora)) {
                horariosDisponibles.add(hora);
            }
        }

        return horariosDisponibles;
    }

    /**
     * Generar horarios por defecto (7:00 - 18:00)
     */
    private List<LocalTime> generarHorariosDefault(LocalDate fecha) {
        // Solo Lunes a Viernes de 7:00 a 18:00
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        if (diaSemana.getValue() >= DayOfWeek.MONDAY.getValue() &&
                diaSemana.getValue() <= DayOfWeek.FRIDAY.getValue()) {

            List<LocalTime> horarios = new ArrayList<>();
            for (LocalTime hora = HORA_INICIO_DEFAULT; hora.isBefore(HORA_FIN_DEFAULT); hora = hora.plusHours(1)) {
                horarios.add(hora);
            }
            return horarios;
        }

        // Fin de semana - sin horarios por defecto
        return new ArrayList<>();
    }

}