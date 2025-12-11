package com.api_caich.service;

import com.api_caich.dto.ticket.CrearTicketDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketFiltroDTO;
import com.api_caich.entity.*;
import com.api_caich.enums.Role;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.*;
import com.api_caich.util.FolioGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final ProductoRepository productoRepository;
    private final TicketDetalleRepository ticketDetalleRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public TicketDTO crearTicketCompleto(CrearTicketDTO ticketDTO, Long usuarioId) {
        log.info("Creating complete ticket for paciente: {}", ticketDTO.getPacienteId());

        // Validar disponibilidad del espacio
        if (ticketDTO.getEspacioId() != null) {
            validarDisponibilidadEspacio(ticketDTO.getEspacioId(),
                    ticketDTO.getFecha(),
                    ticketDTO.getDuracion(),
                    null);
        }

        // Obtener entidades relacionadas
        Usuario paciente = usuarioRepository.findById(ticketDTO.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente not found"));

        Usuario terapeuta = usuarioRepository.findById(ticketDTO.getTerapeutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Terapeuta not found"));

        Espacio espacio = null;
        if (ticketDTO.getEspacioId() != null) {
            espacio = espacioRepository.findById(ticketDTO.getEspacioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Espacio not found"));
        }

        CuentaBancaria cuentaDestino = cuentaBancariaRepository.findById(ticketDTO.getCuentaDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria not found"));

        Usuario creadoPor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador not found"));

        // Generar folio único
        String folio = FolioGenerator.generarFolioTicket();

        // Calcular costo materiales
        BigDecimal costoMateriales = calcularCostoMateriales(ticketDTO.getProductos());

        // Calcular costo total
        BigDecimal costoTotal = ticketDTO.getCostoEspacio()
                .add(costoMateriales)
                .add(ticketDTO.getCostoAdicional());

        // Crear ticket
        Ticket ticket = new Ticket();
        ticket.setFolio(folio);
        ticket.setPaciente(paciente);
        ticket.setTerapeuta(terapeuta);
        ticket.setEspacio(espacio);
        ticket.setCuentaDestino(cuentaDestino);
        ticket.setFecha(ticketDTO.getFecha());
        ticket.setDuracion(ticketDTO.getDuracion());
        ticket.setMateria(ticketDTO.getMateria());
        ticket.setConceptoIngreso(ticketDTO.getConceptoIngreso());
        ticket.setConceptoTransferencia(ticketDTO.getConceptoTransferencia());
        ticket.setMontoPagado(ticketDTO.getMontoPagado());
        ticket.setCelular(ticketDTO.getCelular());
        ticket.setCostoEspacio(ticketDTO.getCostoEspacio());
        ticket.setCostoMateriales(costoMateriales);
        ticket.setCostoAdicional(ticketDTO.getCostoAdicional());
        ticket.setCostoTotal(costoTotal);
        ticket.setCreadoPor(creadoPor);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Procesar productos
        procesarProductosTicket(savedTicket, ticketDTO.getProductos(), usuarioId);

        // Crear notificación
        notificacionService.notificarCitaProxima(paciente.getUsuarioId(),
                savedTicket.getTicketId(),
                ticketDTO.getFecha());

        // Registrar auditoría
        auditoriaService.registrarCambio("Tickets", savedTicket.getTicketId(), "Crear",
                "Todos los campos", null, null, usuarioId, "SYSTEM");

        log.info("Ticket created successfully with folio: {}", folio);
        return mapToDTO(savedTicket);
    }

    private BigDecimal calcularCostoMateriales(List<com.api_caich.dto.ticket.ProductoDetalleDTO> productos) {
        return productos.stream()
                .map(p -> p.getPrecioUnitario().multiply(BigDecimal.valueOf(p.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void procesarProductosTicket(Ticket ticket, List<com.api_caich.dto.ticket.ProductoDetalleDTO> productos,
            Long usuarioId) {
        for (com.api_caich.dto.ticket.ProductoDetalleDTO productoDTO : productos) {
            Producto producto = productoRepository.findById(productoDTO.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto not found"));

            // Validar stock
            if (producto.getStock() < productoDTO.getCantidad()) {
                throw new BusinessException("Stock insuficiente para: " + producto.getNombre() +
                        ". Disponible: " + producto.getStock() +
                        ", Solicitado: " + productoDTO.getCantidad());
            }

            // Reducir stock si no es préstamo
            if ("Venta".equals(productoDTO.getTipoUso()) || "Uso".equals(productoDTO.getTipoUso())) {
                inventarioService.reducirStock(producto.getProductoId(), productoDTO.getCantidad(),
                        "Usado en ticket: " + ticket.getFolio(),
                        "TICKET_" + ticket.getTicketId(), usuarioId);
            }

            // Crear detalle
            TicketDetalle detalle = new TicketDetalle();
            detalle.setTicket(ticket);
            detalle.setProducto(producto);
            detalle.setCantidad(productoDTO.getCantidad());
            detalle.setPrecioUnitario(productoDTO.getPrecioUnitario());
            detalle.setSubtotal(
                    productoDTO.getPrecioUnitario().multiply(BigDecimal.valueOf(productoDTO.getCantidad())));
            detalle.setTipoUso(productoDTO.getTipoUso());

            ticketDetalleRepository.save(detalle);
        }
    }

    public boolean validarDisponibilidadEspacio(Long espacioId, LocalDateTime fechaInicio, Integer duracion,
            Long ticketIdExcluir) {
        if (espacioId == null || duracion == null) {
            return true;
        }

        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        Long conflictCount = ticketRepository.countConflictingTickets(espacioId, fechaInicio, fechaFin,
                ticketIdExcluir);

        return conflictCount == 0;
    }

    @Transactional
    public TicketDTO actualizarEstado(Long ticketId, String estadoTicket, String estadoPago, String notas,
            Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Validar transición de estados
        validarTransicionEstado(ticket.getEstadoTicket(), estadoTicket, ticket.getEstadoPago(), estadoPago);

        String cambios = "";
        if (estadoTicket != null && !estadoTicket.equals(ticket.getEstadoTicket())) {
            cambios += "EstadoTicket: " + ticket.getEstadoTicket() + " -> " + estadoTicket + "; ";
            ticket.setEstadoTicket(estadoTicket);
        }

        if (estadoPago != null && !estadoPago.equals(ticket.getEstadoPago())) {
            cambios += "EstadoPago: " + ticket.getEstadoPago() + " -> " + estadoPago + "; ";
            ticket.setEstadoPago(estadoPago);

            // Notificaciones de pago
            if ("Verificado".equals(estadoPago)) {
                notificacionService.notificarPagoVerificado(ticket.getPaciente().getUsuarioId(),
                        ticketId, ticket.getFolio());
            } else if ("Rechazado".equals(estadoPago)) {
                notificacionService.notificarPagoRechazado(ticket.getPaciente().getUsuarioId(),
                        ticketId, ticket.getFolio(), notas);
            }
        }

        if (notas != null) {
            String nuevasNotas = ticket.getNotas() != null
                    ? ticket.getNotas() + "\n[" + LocalDateTime.now() + "] " + notas
                    : notas;
            ticket.setNotas(nuevasNotas);
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Auditoría
        if (!cambios.isEmpty()) {
            auditoriaService.registrarCambio("Tickets", ticketId, "Actualizar",
                    cambios, null, null, usuarioId, "SYSTEM");
        }

        return mapToDTO(updatedTicket);
    }

    private void validarTransicionEstado(String estadoTicketActual, String estadoTicketNuevo,
            String estadoPagoActual, String estadoPagoNuevo) {
        // Validar transición de estado ticket
        if (estadoTicketNuevo != null) {
            switch (estadoTicketActual) {
                case "Agendado":
                    if (!List.of("Completado", "Cancelado", "NoAsistio").contains(estadoTicketNuevo)) {
                        throw new BusinessException("Transición de estado no permitida: " +
                                estadoTicketActual + " -> " + estadoTicketNuevo);
                    }
                    break;
                case "Completado":
                case "Cancelado":
                case "NoAsistio":
                    throw new BusinessException("No se puede modificar un ticket en estado: " + estadoTicketActual);
            }
        }

        // Validar transición de estado pago
        if (estadoPagoNuevo != null) {
            switch (estadoPagoActual) {
                case "Pendiente":
                    if (!List.of("Pagado", "Rechazado").contains(estadoPagoNuevo)) {
                        throw new BusinessException("Transición de pago no permitida: " +
                                estadoPagoActual + " -> " + estadoPagoNuevo);
                    }
                    break;
                case "Pagado":
                    if (!List.of("Verificado", "Rechazado").contains(estadoPagoNuevo)) {
                        throw new BusinessException("Transición de pago no permitida: " +
                                estadoPagoActual + " -> " + estadoPagoNuevo);
                    }
                    break;
                case "Verificado":
                case "Rechazado":
                    throw new BusinessException("No se puede modificar un pago en estado: " + estadoPagoActual);
            }
        }
    }

    public Page<TicketFiltroDTO> obtenerTicketsConFiltros(
            Long usuarioId,
            String usuarioRol,
            Long pacienteId,
            Long terapeutaId,
            String estadoTicket,
            String estadoPago,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        Page<Ticket> tickets;

        // Aplicar filtros automáticos según rol
        switch (Role.fromNombre(usuarioRol)) {
            case PACIENTE:
                // Paciente solo puede ver sus propios tickets
                tickets = ticketRepository.findByPacienteWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case TERAPEUTA:
                // Terapeuta solo puede ver tickets donde es el terapeuta
                tickets = ticketRepository.findByTerapeutaWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case ADMINISTRADOR:
                // Admin puede ver todos los tickets con filtros opcionales
                tickets = ticketRepository.findWithFilters(
                        pacienteId, terapeutaId, estadoTicket, estadoPago,
                        fechaInicio, fechaFin, pageable);
                break;

            case ALUMNO:
            default:
                // Alumno no tiene acceso a tickets
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return tickets.map(this::mapToFiltroDTO);
    }

    public TicketDTO obtenerTicketConSeguridad(Long ticketId, Long usuarioId, String usuarioRol) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        // Validar acceso según rol
        switch (Role.fromNombre(usuarioRol)) {
            case PACIENTE:
                if (!ticket.getPaciente().getUsuarioId().equals(usuarioId)) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            case TERAPEUTA:
                if (!ticket.getTerapeuta().getUsuarioId().equals(usuarioId)) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            case ADMINISTRADOR:
                // Admin tiene acceso a todos
                break;

            case ALUMNO:
            default:
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return mapToDTO(ticket);
    }

    public Page<TicketDTO> obtenerMisTickets(Long pacienteId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByPacienteUsuarioId(pacienteId, pageable);
        return tickets.map(this::mapToDTO);
    }

    private TicketFiltroDTO mapToFiltroDTO(Ticket ticket) {
        return TicketFiltroDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(ticket.getFolio())
                .fecha(ticket.getFecha())
                .pacienteNombre(ticket.getPaciente().getNombreCompleto())
                .pacienteEmail(ticket.getPaciente().getEmail())
                .terapeutaNombre(ticket.getTerapeuta().getNombreCompleto())
                .terapeutaEmail(ticket.getTerapeuta().getEmail())
                .espacioNombre(ticket.getEspacio() != null ? ticket.getEspacio().getNombre() : "No asignado")
                .materia(ticket.getMateria())
                .costoTotal(ticket.getCostoTotal())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoPago(ticket.getEstadoPago())
                .fechaCreacion(ticket.getFechaCreacion())
                .build();
    }

    private TicketDTO mapToDTO(Ticket ticket) {
        return TicketDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(ticket.getFolio())
                .fecha(ticket.getFecha())
                .pacienteNombre(ticket.getPaciente().getNombreCompleto())
                .terapeutaNombre(ticket.getTerapeuta().getNombreCompleto())
                .espacioNombre(ticket.getEspacio() != null ? ticket.getEspacio().getNombre() : null)
                .costoTotal(ticket.getCostoTotal())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoPago(ticket.getEstadoPago())
                .build();
    }
}