package com.api_caich.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.horario.DisponibilidadDTO;
import com.api_caich.dto.horario.HorarioBaseDTO;
import com.api_caich.dto.horario.SlotDTO;
import com.api_caich.entity.ExcepcionHorario;
import com.api_caich.entity.HorarioBase;
import com.api_caich.entity.TicketEspacio;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ExcepcionHorarioRepository;
import com.api_caich.repository.HorarioBaseRepository;
import com.api_caich.repository.TicketEspacioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio que calcula la disponibilidad de horarios para espacios.
 * 
 * Lógica:
 * 1. Valida que el día esté en diasOperativos del HorarioBase
 * 2. Genera slots según intervaloMinutos entre horaApertura y horaCierre
 * 3. Aplica excepciones (ExcepcionHorario) - bloqueos puntuales
 * 4. Excluye tickets ya reservados con estado no cancelado
 * 5. Retorna lista de slots 100% disponibles
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final HorarioBaseRepository horarioBaseRepository;
    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final TicketEspacioRepository ticketEspacioRepository;
    private final EspacioRepository espacioRepository;

    /**
     * Obtiene los slots disponibles para un espacio en una fecha específica.
     * 
     * @param espacioId ID del espacio
     * @param fecha Fecha para la cual calcular disponibilidad
     * @return DTO con slots disponibles y información de disponibilidad
     */
    @Transactional(readOnly = true)
    public DisponibilidadDTO obtenerSlotsDisponibles(Long espacioId, LocalDate fecha) {
        return obtenerSlotsDisponibles(espacioId, fecha, null);
    }

    /**
     * Obtiene los slots disponibles para un espacio en una fecha específica.
     * Permite excluir un ticket específico (útil al editar).
     * 
     * @param espacioId ID del espacio
     * @param fecha Fecha para la cual calcular disponibilidad
     * @param ticketIdToExclude ID del ticket a excluir de la validación (null si no hay)
     * @return DTO con slots disponibles y información de disponibilidad
     */
    @Transactional(readOnly = true)
    public DisponibilidadDTO obtenerSlotsDisponibles(Long espacioId, LocalDate fecha, Long ticketIdToExclude) {
        log.debug("Calculando disponibilidad para espacio {} en fecha {}", espacioId, fecha);

        // 1. Obtener HorarioBase
        HorarioBase horarioBase = obtenerHorarioBase(espacioId);

        // 2. Validar si el día está operativo
        int diaSemana = fecha.getDayOfWeek().getValue();
        boolean diaOperativo = horarioBase.isDiaOperativo(diaSemana);

        if (!diaOperativo) {
            log.debug("Espacio {} no opera en {} (día: {})", espacioId, fecha, fecha.getDayOfWeek());
            return crearDisponibilidadCerrada(espacioId, fecha, horarioBase,
                "Espacio cerrado este día de la semana");
        }

        // 3. Generar slots base
        List<SlotDTO> slots = generarSlotsBase(horarioBase, fecha);

        // 4. Aplicar excepciones (específicas del espacio + globales)
        List<ExcepcionHorario> exceptacionesEspecificas = excepcionHorarioRepository
            .findByEspacioEspacioIdAndFechaAndEstaActivoTrue(espacioId, fecha);
        
        List<ExcepcionHorario> excepcionesGlobales = excepcionHorarioRepository
            .findExcepcionesGlobalesPorFecha(fecha);
        
        List<ExcepcionHorario> excepciones = new ArrayList<>();
        excepciones.addAll(exceptacionesEspecificas);
        excepciones.addAll(excepcionesGlobales);

        // Obtener nombre del espacio
        String espacioNombre = horarioBase.getEspacio() != null 
            ? horarioBase.getEspacio().getNombre() 
            : espacioRepository.findById(espacioId)
                .map(espacio -> espacio.getNombre())
                .orElse("Espacio " + espacioId);

        slots = aplicarExcepciones(slots, excepciones, fecha, espacioNombre);

        // 5. Excluir tickets reservados
        slots = excluirTicketsReservados(slots, espacioId, fecha, ticketIdToExclude);

        // 6. Armar respuesta
        List<SlotDTO> slotsDisponibles = slots.stream()
            .filter(s -> s.getDisponible() != null && s.getDisponible())
            .collect(Collectors.toList());

        DisponibilidadDTO dto = DisponibilidadDTO.builder()
            .espacioId(espacioId)
            .fecha(fecha)
            .horarioBase(mapToHorarioBaseDTO(horarioBase))
            .horaApertura(horarioBase.getHoraApertura().toString())
            .horaCierre(horarioBase.getHoraCierre().toString())
            .diasOperativo(diaOperativo)
            .abierto(true)
            .slots(slots)
            .totalSlots(slots.size())
            .totalSlotsDisponibles(slotsDisponibles.size())
            .mensaje(slotsDisponibles.isEmpty() ? "No hay disponibilidad para esta fecha" : null)
            .build();

        log.debug("Slots calculados para espacio {}: {} disponibles de {}", 
            espacioId, slotsDisponibles.size(), slots.size());

        return dto;
    }

    /**
     * Obtiene el HorarioBase para un espacio, o el global si no existe el específico.
     */
    private HorarioBase obtenerHorarioBase(Long espacioId) {
        Optional<HorarioBase> horarioEspecifico = horarioBaseRepository.findByEspacioEspacioId(espacioId);
        
        if (horarioEspecifico.isPresent()) {
            return horarioEspecifico.get();
        }

        // Si no existe horario específico, usar el global
        return horarioBaseRepository.findHorarioBaseGlobal()
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró configuración de horario para el espacio"));
    }

    /**
     * Genera la cuadrícula de slots base para una fecha.
     * Todos inician como disponibles hasta que se apliquen excepciones y tickets.
     */
    private List<SlotDTO> generarSlotsBase(HorarioBase horarioBase, LocalDate fecha) {
        List<SlotDTO> slots = new ArrayList<>();

        LocalTime horaActual = horarioBase.getHoraApertura();
        LocalTime horaCierre = horarioBase.getHoraCierre();
        int intervaloMinutos = horarioBase.getIntervaloMinutos();

        while (horaActual.isBefore(horaCierre)) {
            LocalTime horaFin = horaActual.plusMinutes(intervaloMinutos);
            
            if (horaFin.isAfter(horaCierre)) {
                horaFin = horaCierre;
            }

            LocalDateTime inicio = LocalDateTime.of(fecha, horaActual);
            LocalDateTime fin = LocalDateTime.of(fecha, horaFin);

            SlotDTO slot = SlotDTO.builder()
                .inicio(inicio)
                .fin(fin)
                .disponible(true)
                .build();

            slots.add(slot);
            horaActual = horaFin;
        }

        return slots;
    }

    /**
     * Aplica las excepciones a los slots.
     * Los slots que caen dentro de un bloqueo se marcan como no disponibles.
     */
    private List<SlotDTO> aplicarExcepciones(List<SlotDTO> slots, List<ExcepcionHorario> excepciones, 
                                              LocalDate fecha, String espacioNombre) {
        for (SlotDTO slot : slots) {
            for (ExcepcionHorario excepcion : excepciones) {
                if (!excepcion.getEstaActivo()) {
                    continue;
                }

                if (excepcion.getTodoElDia()) {
                    // Si es cierre todo el día, marcar todo como no disponible
                    slot.setDisponible(false);
                    slot.setMotivo(excepcion.getMotivo() != null ? excepcion.getMotivo() : "Cerrado");
                    break;
                } else if (excepcion.getHoraInicioBloqueo() != null && excepcion.getHoraFinBloqueo() != null) {
                    // Verificar si el slot se solapa con el bloqueo
                    if (excepcion.estaBloqueado(slot.getInicio().toLocalTime(), slot.getFin().toLocalTime())) {
                        slot.setDisponible(false);
                        slot.setMotivo(excepcion.getMotivo() != null ? excepcion.getMotivo() : "Bloqueado");
                        break;
                    }
                }
            }
        }

        return slots;
    }

    /**
     * Excluye los timeslots que ya tienen tickets reservados (no cancelados).
     * 
     * @param ticketIdToExclude ID del ticket a excluir de la validación (útil al editar)
     */
    private List<SlotDTO> excluirTicketsReservados(List<SlotDTO> slots, Long espacioId, LocalDate fecha, Long ticketIdToExclude) {
        // Obtener todos los tickets para el espacio en esa fecha
        LocalDateTime inicioDelDia = LocalDateTime.of(fecha, LocalTime.MIDNIGHT);
        LocalDateTime finDelDia = LocalDateTime.of(fecha, LocalTime.MAX);

        List<TicketEspacio> ticketsReservados = ticketEspacioRepository
            .findByHoraInicioReservaBetween(inicioDelDia, finDelDia).stream()
            .filter(te -> te.getEspacio().getEspacioId().equals(espacioId))
            .filter(te -> te.getTicket().getEstadoTicket() != EstadoTicket.CANCELADO
                    && te.getTicket().getEstadoTicket() != EstadoTicket.NO_ASISTIO)
            .filter(te -> ticketIdToExclude == null || !te.getTicket().getTicketId().equals(ticketIdToExclude))
            .collect(Collectors.toList());

        for (SlotDTO slot : slots) {
            if (slot.getDisponible()) {
                for (TicketEspacio ticket : ticketsReservados) {
                    if (hySolapamiento(slot.getInicio(), slot.getFin(), 
                                       ticket.getHoraInicioReserva(), ticket.getHoraFinReserva())) {
                        slot.setDisponible(false);
                        slot.setMotivo("Reservado");
                        break;
                    }
                }
            }
        }

        return slots;
    }

    /**
     * Verifica si dos rangos de tiempo se solapan.
     */
    private boolean hySolapamiento(LocalDateTime inicio1, LocalDateTime fin1, 
                                  LocalDateTime inicio2, LocalDateTime fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    /**
     * Crea un DTO de disponibilidad para cuando está completamente cerrado.
     */
    private DisponibilidadDTO crearDisponibilidadCerrada(Long espacioId, LocalDate fecha, HorarioBase horarioBase,
                                                         String mensaje) {
        return DisponibilidadDTO.builder()
            .espacioId(espacioId)
            .fecha(fecha)
            .horarioBase(mapToHorarioBaseDTO(horarioBase))
            .horaApertura(horarioBase.getHoraApertura().toString())
            .horaCierre(horarioBase.getHoraCierre().toString())
            .diasOperativo(false)
            .abierto(false)
            .slots(new ArrayList<>())
            .totalSlots(0)
            .totalSlotsDisponibles(0)
            .mensaje(mensaje)
            .build();
    }

    private HorarioBaseDTO mapToHorarioBaseDTO(HorarioBase horarioBase) {
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

    /**
     * Verifica si una hora específica está disponible para un espacio.
     * Útil para validaciones rápidas.
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long espacioId, LocalDateTime horaInicio, LocalDateTime horaFin) {
        return estaDisponible(espacioId, horaInicio, horaFin, null);
    }

    /**
     * Verifica si una hora específica está disponible para un espacio.
     * Permite excluir un ticket específico de la validación.
     * 
     * @param ticketIdToExclude ID del ticket a excluir (null para no excluir ninguno)
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long espacioId, LocalDateTime horaInicio, LocalDateTime horaFin, Long ticketIdToExclude) {
        DisponibilidadDTO disponibilidad = obtenerSlotsDisponibles(espacioId, horaInicio.toLocalDate(), ticketIdToExclude);

        if (!disponibilidad.getAbierto()) {
            return false;
        }

        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            return false;
        }

        List<SlotDTO> slotsEnRango = disponibilidad.getSlots().stream()
            .filter(slot -> {
                LocalDateTime inicio = slot.getInicio();
                LocalDateTime fin = slot.getFin();
                return (inicio.equals(horaInicio) || inicio.isAfter(horaInicio))
                    && (fin.equals(horaFin) || fin.isBefore(horaFin));
            })
            .collect(Collectors.toList());

        if (slotsEnRango.isEmpty()) {
            return false;
        }

        boolean cubreInicio = slotsEnRango.stream()
            .anyMatch(slot -> slot.getInicio().equals(horaInicio));
        boolean cubreFin = slotsEnRango.stream()
            .anyMatch(slot -> slot.getFin().equals(horaFin));

        if (!cubreInicio || !cubreFin) {
            return false;
        }

        return slotsEnRango.stream().allMatch(slot -> Boolean.TRUE.equals(slot.getDisponible()));
    }
}
