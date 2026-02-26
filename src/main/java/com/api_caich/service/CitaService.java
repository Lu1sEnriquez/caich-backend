package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.cita.ActualizarCitaRequestDTO;
import com.api_caich.dto.cita.CitaResponseDTO;
import com.api_caich.dto.cita.CrearCitaRequestDTO;
import com.api_caich.dto.cita.ProductoCitaDTO;
import com.api_caich.dto.ticket.TicketCitaDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketEspacioDTO;
import com.api_caich.dto.ticket.TicketPrestamoDTO;
import com.api_caich.dto.ticket.TicketRequestDTO;
import com.api_caich.dto.ticket.TicketVentaDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.TicketCita;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.TicketCitaRepository;
import com.api_caich.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio puente para la API de citas del frontend.
 * Transforma los DTOs de cita en TicketRequestDTO y delega al TicketService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final TicketCitaRepository ticketCitaRepository;
    private final EspacioRepository espacioRepository;
    private final ProductoRepository productoRepository;

    /**
     * Crear una cita transformándola en un Ticket con componente Cita.
     */
    @Transactional
    public CitaResponseDTO crearCita(CrearCitaRequestDTO request, Long usuarioCreadorId) {
        log.info("Creando cita para paciente {} con terapeuta {}", request.getPacienteId(), request.getTerapeutaId());

        LocalDateTime fechaInicio = parseFecha(request.getFecha());
        LocalDateTime fechaFin = fechaInicio.plusMinutes(request.getDuracion());

        // Construir TicketRequestDTO
        TicketRequestDTO ticketRequest = new TicketRequestDTO();
        ticketRequest.setCreadoPorId(usuarioCreadorId);
        ticketRequest.setEstadoTicket(EstadoTicket.AGENDADO);

        // Construir componente cita
        TicketCitaDTO citaDTO = TicketCitaDTO.builder()
                .pacienteId(request.getPacienteId())
                .terapeutaId(request.getTerapeutaId())
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .precioCobrado(BigDecimal.ZERO)
                .notasTerapia(request.getNotas())
                .modalidad(request.getModalidad() != null ? request.getModalidad() : "Presencial")
                .build();
        ticketRequest.setCita(citaDTO);

        // Construir componente espacio si es presencial
        if (!"Online".equalsIgnoreCase(request.getModalidad()) && request.getEspacioId() != null) {
            Espacio espacio = espacioRepository.findById(request.getEspacioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado: " + request.getEspacioId()));

            BigDecimal costoEspacio = calcularCostoEspacio(espacio, request.getDuracion());

            TicketEspacioDTO espacioDTO = new TicketEspacioDTO();
            espacioDTO.setEspacioId(request.getEspacioId());
            espacioDTO.setHoraInicioReserva(fechaInicio);
            espacioDTO.setHoraFinReserva(fechaFin);
            espacioDTO.setCostoCobrado(costoEspacio);
            ticketRequest.setEspacios(List.of(espacioDTO));
        }

        // Procesar productos (ventas y préstamos)
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            List<TicketVentaDTO> ventas = new ArrayList<>();
            List<TicketPrestamoDTO> prestamos = new ArrayList<>();

            for (ProductoCitaDTO prod : request.getProductos()) {
                Producto producto = productoRepository.findById(prod.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + prod.getProductoId()));

                if ("Venta".equalsIgnoreCase(prod.getTipoUso()) || "Uso".equalsIgnoreCase(prod.getTipoUso())) {
                    TicketVentaDTO venta = new TicketVentaDTO();
                    venta.setProductoId(prod.getProductoId());
                    venta.setCantidad(prod.getCantidad() != null ? prod.getCantidad() : 1);
                    venta.setPrecioUnitario(prod.getPrecioUnitario() != null ? prod.getPrecioUnitario() : producto.getPrecio());
                    ventas.add(venta);
                } else if ("Prestamo".equalsIgnoreCase(prod.getTipoUso())) {
                    TicketPrestamoDTO prestamo = new TicketPrestamoDTO();
                    prestamo.setProductoId(prod.getProductoId());
                    prestamo.setResponsableId(request.getPacienteId());
                    prestamo.setFechaPrestamo(fechaInicio);
                    if (prod.getFechaDevolucionEstimada() != null) {
                        prestamo.setFechaDevolucionEstimada(parseFecha(prod.getFechaDevolucionEstimada()));
                    } else {
                        prestamo.setFechaDevolucionEstimada(fechaFin);
                    }
                    prestamos.add(prestamo);
                }
            }

            ticketRequest.setVentas(ventas);
            ticketRequest.setPrestamos(prestamos);
        }

        // Costo adicional
        if (request.getCostoAdicional() != null) {
            ticketRequest.setCostoAdicional(request.getCostoAdicional());
        }

        // Crear ticket vía TicketService
        TicketDTO ticketDTO = ticketService.crearTicket(ticketRequest);

        return mapTicketToCitaResponse(ticketDTO, request.getEspacioId(), request.getDuracion());
    }

    /**
     * Actualizar una cita (actualiza estado del ticket y/o datos de la cita).
     */
    @Transactional
    public CitaResponseDTO actualizarCita(Long citaId, ActualizarCitaRequestDTO request, Long usuarioId) {
        log.info("Actualizando cita con id: {}", citaId);

        // Buscar la cita por su ID
        TicketCita cita = ticketCitaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + citaId));

        Ticket ticket = cita.getTicket();

        // Actualizar estado si se proporciona
        if (request.getEstadoTicket() != null) {
            try {
                EstadoTicket nuevoEstado = EstadoTicket.valueOf(request.getEstadoTicket());
                ticketService.actualizarEstado(ticket.getTicketId(), nuevoEstado, request.getNotas(), usuarioId);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Estado de ticket inválido: " + request.getEstadoTicket());
            }
        }

        // Actualizar datos de la cita
        boolean citaModificada = false;

        if (request.getPacienteId() != null) {
            cita.getPaciente().setUsuarioId(request.getPacienteId());
            citaModificada = true;
        }
        if (request.getTerapeutaId() != null) {
            cita.getTerapeuta().setUsuarioId(request.getTerapeutaId());
            citaModificada = true;
        }
        if (request.getFecha() != null) {
            LocalDateTime nuevaFechaInicio = parseFecha(request.getFecha());
            int duracion = request.getDuracion() != null ? request.getDuracion() : 
                    (int) Duration.between(cita.getFechaInicio(), cita.getFechaFin()).toMinutes();
            LocalDateTime nuevaFechaFin = nuevaFechaInicio.plusMinutes(duracion);
            cita.setFechaInicio(nuevaFechaInicio);
            cita.setFechaFin(nuevaFechaFin);
            citaModificada = true;
        } else if (request.getDuracion() != null) {
            cita.setFechaFin(cita.getFechaInicio().plusMinutes(request.getDuracion()));
            citaModificada = true;
        }
        if (request.getModalidad() != null) {
            cita.setModalidad(request.getModalidad());
            citaModificada = true;
        }
        if (request.getNotas() != null) {
            cita.setNotasTerapia(request.getNotas());
            citaModificada = true;
        }

        if (citaModificada) {
            ticketCitaRepository.save(cita);
        }

        // Recargar ticket completo
        Ticket ticketActualizado = ticketRepository.findById(ticket.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        TicketDTO ticketDTO = TicketDTO.fromEntity(ticketActualizado);
        int duracion = request.getDuracion() != null ? request.getDuracion() :
                (int) Duration.between(cita.getFechaInicio(), cita.getFechaFin()).toMinutes();
        Long espacioId = request.getEspacioId() != null ? request.getEspacioId() : null;

        return mapTicketToCitaResponse(ticketDTO, espacioId, duracion);
    }

    /**
     * Eliminar/cancelar una cita.
     */
    @Transactional
    public void eliminarCita(Long citaId, Long usuarioId) {
        log.info("Eliminando cita con id: {}", citaId);

        TicketCita cita = ticketCitaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + citaId));

        Ticket ticket = cita.getTicket();

        // Cancelar el ticket asociado
        ticketService.cancelarTicket(ticket.getTicketId(), "Cita eliminada desde calendario", usuarioId);
    }

    /**
     * Obtener citas por terapeuta.
     */
    public List<CitaResponseDTO> obtenerCitasPorTerapeuta(Long terapeutaId) {
        List<TicketCita> citas = ticketCitaRepository.findByTerapeuta_UsuarioId(terapeutaId);
        return citas.stream()
                .filter(c -> c.getTicket().getEstadoTicket() != EstadoTicket.CANCELADO)
                .map(this::mapEntityToCitaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener citas por paciente.
     */
    public List<CitaResponseDTO> obtenerCitasPorPaciente(Long pacienteId) {
        List<TicketCita> citas = ticketCitaRepository.findByPaciente_UsuarioId(pacienteId);
        return citas.stream()
                .filter(c -> c.getTicket().getEstadoTicket() != EstadoTicket.CANCELADO)
                .map(this::mapEntityToCitaResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // Helpers
    // ============================================================

    private LocalDateTime parseFecha(String fecha) {
        if (fecha == null) throw new BusinessException("La fecha es requerida");
        try {
            return LocalDateTime.parse(fecha, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Try ISO_LOCAL_DATE_TIME without zone
                return LocalDateTime.parse(fecha);
            } catch (DateTimeParseException e2) {
                throw new BusinessException("Formato de fecha inválido: " + fecha + ". Use formato ISO: yyyy-MM-ddTHH:mm:ss");
            }
        }
    }

    private BigDecimal calcularCostoEspacio(Espacio espacio, int duracionMinutos) {
        if (espacio.getCostoPorHora() == null || espacio.getCostoPorHora().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        double horas = duracionMinutos / 60.0;
        return espacio.getCostoPorHora().multiply(BigDecimal.valueOf(horas));
    }

    private CitaResponseDTO mapTicketToCitaResponse(TicketDTO ticket, Long espacioId, Integer duracion) {
        var builder = CitaResponseDTO.builder()
                .ticketId(ticket.getTicketId())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoPago(ticket.getEstadoFinanciero())
                .montoPagado(ticket.getMontoPagado())
                .costoTotal(ticket.getMontoTotal())
                .costoAdicional(ticket.getCostoAdicional())
                .duracion(duracion)
                .fechaCreacion(ticket.getFechaCreacion() != null ? ticket.getFechaCreacion().toString() : null);

        // Cita info
        if (ticket.getCita() != null) {
            builder.pacienteId(ticket.getCita().getPacienteId())
                    .pacienteNombre(ticket.getCita().getPacienteNombre())
                    .terapeutaId(ticket.getCita().getTerapeutaId())
                    .terapeutaNombre(ticket.getCita().getTerapeutaNombre())
                    .modalidad(ticket.getCita().getModalidad())
                    .notas(ticket.getCita().getNotasTerapia())
                    .fecha(ticket.getCita().getFechaInicio() != null ? ticket.getCita().getFechaInicio().toString() : null);
        }

        // Espacio info
        if (ticket.getEspacios() != null && !ticket.getEspacios().isEmpty()) {
            var espacio = ticket.getEspacios().get(0);
            builder.espacioId(espacio.getEspacioId())
                    .espacioNombre(espacio.getEspacioNombre())
                    .costoEspacio(espacio.getCostoCobrado());
        } else if (espacioId != null) {
            builder.espacioId(espacioId);
        }

        return builder.build();
    }

    private CitaResponseDTO mapEntityToCitaResponse(TicketCita cita) {
        Ticket ticket = cita.getTicket();
        int duracion = (int) Duration.between(cita.getFechaInicio(), cita.getFechaFin()).toMinutes();

        var builder = CitaResponseDTO.builder()
                .ticketId(ticket.getTicketId())
                .fecha(cita.getFechaInicio().toString())
                .duracion(duracion)
                .pacienteId(cita.getPaciente() != null ? cita.getPaciente().getUsuarioId() : null)
                .pacienteNombre(cita.getPaciente() != null ? cita.getPaciente().getNombreCompleto() : null)
                .terapeutaId(cita.getTerapeuta() != null ? cita.getTerapeuta().getUsuarioId() : null)
                .terapeutaNombre(cita.getTerapeuta() != null ? cita.getTerapeuta().getNombreCompleto() : null)
                .modalidad(cita.getModalidad())
                .notas(cita.getNotasTerapia())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoPago(ticket.getEstadoFinanciero() != null ? ticket.getEstadoFinanciero() : null)
                .montoPagado(ticket.getMontoPagado())
                .costoTotal(ticket.getMontoTotal())
                .costoAdicional(ticket.getCostoAdicional())
                .fechaCreacion(ticket.getFechaCreacion() != null ? ticket.getFechaCreacion().toString() : null);

        // Espacio info
        if (ticket.getEspacios() != null && !ticket.getEspacios().isEmpty()) {
            var espacio = ticket.getEspacios().get(0);
            builder.espacioId(espacio.getEspacio().getEspacioId())
                    .espacioNombre(espacio.getEspacio().getNombre())
                    .costoEspacio(espacio.getCostoCobrado());
        }

        return builder.build();
    }
}
