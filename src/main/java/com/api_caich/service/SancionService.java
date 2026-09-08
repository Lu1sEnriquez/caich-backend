package com.api_caich.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.sancion.CrearSancionDTO;
import com.api_caich.dto.sancion.SancionDTO;
import com.api_caich.entity.Sancion;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoSancion;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.SancionRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SancionService {

    private final SancionRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public SancionDTO crearSancion(CrearSancionDTO request, Long creadoPorId) {
        log.info("Creando sanción para alumno ID: {}", request.getAlumnoId());

        // Validar alumno
        Usuario alumno = usuarioRepository.findById(request.getAlumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        // Validar usuario creador
        Usuario creadoPor = usuarioRepository.findById(creadoPorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        // Crear sanción
        Sancion sancion = new Sancion();
        sancion.setAlumno(alumno);
        sancion.setTipoSancion(request.getTipoSancion());
        sancion.setMotivo(request.getMotivo());
        sancion.setDescripcion(request.getDescripcion());
        sancion.setFechaInicio(request.getFechaInicio());
        sancion.setFechaFin(request.getFechaFin());
        sancion.setEstado(EstadoSancion.ACTIVA);
        sancion.setCreadoPor(creadoPor);

        if (request.getTicketRelacionadoId() != null) {
            Ticket ticket = ticketRepository.findById(request.getTicketRelacionadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
            sancion.setTicketRelacionado(ticket);
        }

        sancion = sancionRepository.save(sancion);

        // Enviar notificación al alumno
        notificacionService.notificarSancionAplicada(
                alumno.getUsuarioId(),
                sancion.getSancionId(),
                request.getMotivo()
        );

        log.info("Sanción creada exitosamente: ID {}", sancion.getSancionId());

        return mapToDTO(sancion);
    }

    @Transactional(readOnly = true)
    public List<SancionDTO> obtenerTodasSanciones(String estado) {
        log.info("Obteniendo todas las sanciones");

        List<Sancion> sanciones;
        if (estado != null) {
            EstadoSancion estadoEnum = EstadoSancion.valueOf(estado);
            sanciones = sancionRepository.findByEstado(estadoEnum);
        } else {
            sanciones = sancionRepository.findAll();
        }

        return sanciones.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SancionDTO> obtenerSancionesAlumno(Long alumnoId) {
        log.info("Obteniendo sanciones para alumno ID: {}", alumnoId);

        List<Sancion> sanciones = sancionRepository.findByAlumno_UsuarioId(alumnoId);

        return sanciones.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SancionDTO obtenerSancion(Long sancionId) {
        log.info("Obteniendo sanción ID: {}", sancionId);

        Sancion sancion = sancionRepository.findById(sancionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sanción no encontrada"));

        return mapToDTO(sancion);
    }

    @Transactional
    public SancionDTO anularSancion(Long sancionId, String motivoAnulacion, Long anuladoPorId) {
        log.info("Anulando sanción ID: {}", sancionId);

        Sancion sancion = sancionRepository.findById(sancionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sanción no encontrada"));

        if (sancion.getEstado() != EstadoSancion.ACTIVA) {
            throw new BusinessException("Solo se pueden anular sanciones activas");
        }

        Usuario anuladoPor = usuarioRepository.findById(anuladoPorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario anulador no encontrado"));

        sancion.setEstado(EstadoSancion.ANULADA);
        sancion.setMotivoAnulacion(motivoAnulacion);
        sancion.setAnuladoPor(anuladoPor);
        sancion.setFechaAnulacion(LocalDateTime.now());

        sancion = sancionRepository.save(sancion);

        log.info("Sanción anulada exitosamente: ID {}", sancionId);

        return mapToDTO(sancion);
    }

    @Transactional
    public SancionDTO cumplirSancion(Long sancionId) {
        log.info("Marcando sanción como cumplida ID: {}", sancionId);

        Sancion sancion = sancionRepository.findById(sancionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sanción no encontrada"));

        if (sancion.getEstado() != EstadoSancion.ACTIVA) {
            throw new BusinessException("Solo se pueden marcar como cumplidas sanciones activas");
        }

        sancion.setEstado(EstadoSancion.CUMPLIDA);
        sancion = sancionRepository.save(sancion);

        log.info("Sanción marcada como cumplida: ID {}", sancionId);

        return mapToDTO(sancion);
    }

    @Transactional
    public void eliminarSancion(Long sancionId) {
        log.info("Eliminando sanción ID: {}", sancionId);

        if (!sancionRepository.existsById(sancionId)) {
            throw new ResourceNotFoundException("Sanción no encontrada");
        }

        sancionRepository.deleteById(sancionId);

        log.info("Sanción eliminada exitosamente: ID {}", sancionId);
    }

    private SancionDTO mapToDTO(Sancion sancion) {
        return SancionDTO.builder()
                .sancionId(sancion.getSancionId())
                .alumnoId(sancion.getAlumno().getUsuarioId())
                .alumnoNombre(sancion.getAlumno().getNombreCompleto())
                .tipoSancion(sancion.getTipoSancion())
                .motivo(sancion.getMotivo())
                .descripcion(sancion.getDescripcion())
                .fechaInicio(sancion.getFechaInicio())
                .fechaFin(sancion.getFechaFin())
                .estado(sancion.getEstado())
                .creadoPorNombre(sancion.getCreadoPor().getNombreCompleto())
                .fechaCreacion(sancion.getFechaCreacion())
                .anuladoPorNombre(sancion.getAnuladoPor() != null ?
                        sancion.getAnuladoPor().getNombreCompleto() : null)
                .fechaAnulacion(sancion.getFechaAnulacion())
                .motivoAnulacion(sancion.getMotivoAnulacion())
                .ticketRelacionadoId(sancion.getTicketRelacionado() != null ?
                        sancion.getTicketRelacionado().getTicketId() : null)
                .ticketFolio(sancion.getTicketRelacionado() != null ?
                        sancion.getTicketRelacionado().getFolio() : null)
                .build();
    }
}
