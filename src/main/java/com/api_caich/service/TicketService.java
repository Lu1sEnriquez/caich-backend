package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.ticket.TicketCitaDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketEspacioDTO;
import com.api_caich.dto.ticket.TicketFiltroDTO;
import com.api_caich.dto.ticket.TicketPrestamoDTO;
import com.api_caich.dto.ticket.TicketRequestDTO;
import com.api_caich.dto.ticket.TicketVentaDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.Pago;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.TicketCita;
import com.api_caich.entity.TicketEspacio;
import com.api_caich.entity.TicketPrestamo;
import com.api_caich.entity.TicketVenta;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.EstadoPrestamo;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.enums.Role;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.TicketCitaRepository;
import com.api_caich.repository.TicketEspacioRepository;
import com.api_caich.repository.TicketPrestamoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.TicketVentaRepository;
import com.api_caich.repository.UsuarioRepository;
import com.api_caich.util.FolioGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestión de Tickets con Arquitectura Modular de Composición
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ProductoRepository productoRepository;
    private final TicketVentaRepository ticketVentaRepository;
    private final TicketCitaRepository ticketCitaRepository;
    private final TicketEspacioRepository ticketEspacioRepository;
    private final TicketPrestamoRepository ticketPrestamoRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final PagoService pagoService;
    private final FolioGenerator folioGenerator;

    /**
     * Crear ticket con Arquitectura Modular de Composición
     * Soporta Ventas, Citas, Espacios y Préstamos en un solo ticket
     */
    @Transactional
    public TicketDTO crearTicket(TicketRequestDTO request) {
        log.info("Creando ticket modular para usuario: {}", request.getCreadoPorId());

        // 1. Validar y obtener el usuario creador
        Usuario creadoPor = usuarioRepository.findById(request.getCreadoPorId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        // 2. Generar folio único
        String folio = folioGenerator.generarFolioTicket();

        // 3. Crear la entidad Ticket principal
        Ticket ticket = new Ticket();
        ticket.setFolio(folio);
        ticket.setCreadoPor(creadoPor);
        ticket.setEstadoTicket(request.getEstadoTicket());
        ticket.setEstadoFinanciero(request.getEstadoFinanciero());
        ticket.setCostoAdicional(request.getCostoAdicional() != null ? request.getCostoAdicional() : BigDecimal.ZERO);
        ticket.setMotivoCostoAdicional(request.getMotivoCostoAdicional());
        ticket.setMontoPagado(BigDecimal.ZERO);
        ticket.setVentas(new ArrayList<>());
        ticket.setEspacios(new ArrayList<>());
        ticket.setPrestamos(new ArrayList<>());
        ticket.setPagos(new ArrayList<>());

        // 4. Procesar VENTAS (Productos)
        if (request.getVentas() != null && !request.getVentas().isEmpty()) {
            for (TicketVentaDTO ventaDTO : request.getVentas()) {
                Producto producto = productoRepository.findById(ventaDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + ventaDTO.getProductoId()));

                if (producto.getStock() < ventaDTO.getCantidad()) {
                    throw new BusinessException("Stock insuficiente para: " + producto.getNombre()
                            + ". Disponible: " + producto.getStock()
                            + ", Solicitado: " + ventaDTO.getCantidad());
                }

                TicketVenta venta = new TicketVenta();
                venta.setTicket(ticket);
                venta.setProducto(producto);
                venta.setCantidad(ventaDTO.getCantidad());
                venta.setPrecioUnitario(ventaDTO.getPrecioUnitario());
                venta.setSubtotal(ventaDTO.getPrecioUnitario().multiply(BigDecimal.valueOf(ventaDTO.getCantidad())));
                ticket.getVentas().add(venta);

                inventarioService.reducirStock(producto.getProductoId(), ventaDTO.getCantidad(),
                        "Venta en ticket: " + folio, "TICKET_VENTA_" + folio, request.getCreadoPorId());
            }
        }

        // 5. Procesar CITA (máximo 1 por ticket)
        if (request.getCita() != null) {
            TicketCitaDTO citaDTO = request.getCita();
            Usuario paciente = null;
            if (citaDTO.getPacienteId() != null) {
                paciente = usuarioRepository.findById(citaDTO.getPacienteId())
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            }
            Usuario terapeuta = null;
            if (citaDTO.getTerapeutaId() != null) {
                terapeuta = usuarioRepository.findById(citaDTO.getTerapeutaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Terapeuta no encontrado"));
            }

            // Validar conflictos de horarios
            if (paciente != null && citaDTO.getFechaInicio() != null && citaDTO.getFechaFin() != null) {
                Long conflictos = ticketCitaRepository.countConflictingCitasForPaciente(
                    paciente.getUsuarioId(),
                    citaDTO.getFechaInicio(),
                    citaDTO.getFechaFin(),
                    List.of(EstadoTicket.CANCELADO),
                    null
                );
                if (conflictos > 0) {
                    throw new BusinessException("El paciente ya tiene una cita en ese horario");
                }
            }

            if (terapeuta != null && citaDTO.getFechaInicio() != null && citaDTO.getFechaFin() != null) {
                Long conflictos = ticketCitaRepository.countConflictingCitasForTerapeuta(
                    terapeuta.getUsuarioId(),
                    citaDTO.getFechaInicio(),
                    citaDTO.getFechaFin(),
                    List.of(EstadoTicket.CANCELADO),
                    null
                );
                if (conflictos > 0) {
                    throw new BusinessException("El terapeuta ya tiene una cita en ese horario");
                }
            }

            TicketCita cita = new TicketCita();
            cita.setTicket(ticket);
            cita.setPaciente(paciente);
            cita.setTerapeuta(terapeuta);
            cita.setFechaInicio(citaDTO.getFechaInicio());
            cita.setFechaFin(citaDTO.getFechaFin());
            cita.setPrecioCobrado(citaDTO.getPrecioCobrado());
            cita.setNotasTerapia(citaDTO.getNotasTerapia());
            ticket.setCita(cita);
        }

        // 6. Procesar ESPACIOS (Renta de cubículos)
        if (request.getEspacios() != null && !request.getEspacios().isEmpty()) {
            for (TicketEspacioDTO espacioDTO : request.getEspacios()) {
                Espacio espacio = espacioRepository.findById(espacioDTO.getEspacioId())
                        .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado: " + espacioDTO.getEspacioId()));

                // Validar conflictos de reservas
                if (espacioDTO.getHoraInicioReserva() != null && espacioDTO.getHoraFinReserva() != null) {
                    Long conflictos = ticketEspacioRepository.countConflictingReservas(
                        espacio.getEspacioId(),
                        espacioDTO.getHoraInicioReserva(),
                        espacioDTO.getHoraFinReserva(),
                        List.of(EstadoTicket.CANCELADO),
                        null
                    );
                    if (conflictos > 0) {
                        throw new BusinessException("El espacio ya está reservado en ese horario");
                    }
                }

                TicketEspacio ticketEspacio = new TicketEspacio();
                ticketEspacio.setTicket(ticket);
                ticketEspacio.setEspacio(espacio);
                ticketEspacio.setHoraInicioReserva(espacioDTO.getHoraInicioReserva());
                ticketEspacio.setHoraFinReserva(espacioDTO.getHoraFinReserva());
                ticketEspacio.setCostoCobrado(espacioDTO.getCostoCobrado());
                ticket.getEspacios().add(ticketEspacio);
            }
        }

        // 7. Procesar PRÉSTAMOS
        if (request.getPrestamos() != null && !request.getPrestamos().isEmpty()) {
            for (TicketPrestamoDTO prestamoDTO : request.getPrestamos()) {
                Producto producto = productoRepository.findById(prestamoDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + prestamoDTO.getProductoId()));

                if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
                    throw new BusinessException("El producto no es prestable: " + producto.getNombre());
                }

                Usuario responsable = usuarioRepository.findById(prestamoDTO.getResponsableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Responsable no encontrado"));

                TicketPrestamo prestamo = new TicketPrestamo();
                prestamo.setTicket(ticket);
                prestamo.setProducto(producto);
                prestamo.setResponsable(responsable);
                prestamo.setFechaPrestamo(prestamoDTO.getFechaPrestamo());
                prestamo.setFechaDevolucionEstimada(prestamoDTO.getFechaDevolucionEstimada());
                prestamo.setEstado(EstadoPrestamo.ACTIVO);
                ticket.getPrestamos().add(prestamo);
            }
        }

        // 8. Recalcular el total del ticket
        ticket.recalcularTotal();

        // 9. Guardar todo en una sola transacción
        Ticket ticketGuardado = ticketRepository.save(ticket);

        // 10. Registrar auditoría
        auditoriaService.registrarCambio("Tickets", ticketGuardado.getTicketId(), "Crear",
                "Ticket modular creado con folio: " + folio, null, null, request.getCreadoPorId(), "SYSTEM");

        // 11. Crear notificaciones si hay cita
        if (ticketGuardado.getCita() != null && ticketGuardado.getCita().getPaciente() != null) {
            notificacionService.notificarCitaProxima(
                    ticketGuardado.getCita().getPaciente().getUsuarioId(),
                    ticketGuardado.getTicketId(),
                    ticketGuardado.getCita().getFechaInicio());
        }

        log.info("Ticket modular creado exitosamente con folio: {}", folio);
        return mapToDTO(ticketGuardado);
    }

    /**
     * Actualizar estado del ticket
     */
    @Transactional
    public TicketDTO actualizarEstado(Long ticketId, EstadoTicket nuevoEstado, String notas, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (nuevoEstado != null && !nuevoEstado.equals(ticket.getEstadoTicket())) {
            // Validar transiciones permitidas
            validarTransicionEstado(ticket.getEstadoTicket(), nuevoEstado);

            EstadoTicket estadoAnterior = ticket.getEstadoTicket();
            ticket.setEstadoTicket(nuevoEstado);

            auditoriaService.registrarCambio("Tickets", ticketId, "Actualizar",
                    "EstadoTicket: " + estadoAnterior + " -> " + nuevoEstado,
                    null, null, usuarioId, "SYSTEM");
        }

        Ticket ticketActualizado = ticketRepository.save(ticket);
        return mapToDTO(ticketActualizado);
    }

    /**
     * Cancelar ticket y revertir operaciones (stock, préstamos)
     */
    @Transactional
    public void cancelarTicket(Long ticketId, String motivo, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO) {
            throw new BusinessException("El ticket ya está cancelado");
        }

        if (ticket.getEstadoTicket() == EstadoTicket.COMPLETADO) {
            throw new BusinessException("No se puede cancelar un ticket completado");
        }

        // Cambiar estado
        ticket.setEstadoTicket(EstadoTicket.CANCELADO);

        // Devolver stock de ventas
        if (ticket.getVentas() != null) {
            for (TicketVenta venta : ticket.getVentas()) {
                inventarioService.incrementarStock(
                    venta.getProducto().getProductoId(),
                    venta.getCantidad(),
                    "Devolución por cancelación de ticket: " + ticket.getFolio(),
                    "TICKET_CANCEL_" + ticket.getTicketId(),
                    usuarioId
                );
            }
        }

        // Cancelar préstamos activos
        if (ticket.getPrestamos() != null) {
            for (TicketPrestamo prestamo : ticket.getPrestamos()) {
                if (EstadoPrestamo.ACTIVO.equals(prestamo.getEstado()) || EstadoPrestamo.VENCIDO.equals(prestamo.getEstado())) {
                    prestamo.setEstado(EstadoPrestamo.PERDIDO);
                    prestamo.setFechaDevolucionReal(LocalDateTime.now());
                }
            }
        }

        ticketRepository.save(ticket);

        // Notificar cancelación si hay cita
        if (ticket.getCita() != null && ticket.getCita().getPaciente() != null) {
            notificacionService.notificarCancelacionCita(
                ticket.getCita().getPaciente().getUsuarioId(),
                ticketId,
                ticket.getFolio(),
                motivo
            );
        }

        auditoriaService.registrarCambio("Tickets", ticketId, "Cancelar",
                "Ticket cancelado - Motivo: " + motivo, null, null, usuarioId, "SYSTEM");

        log.info("Ticket {} cancelado. Motivo: {}", ticket.getFolio(), motivo);
    }

    /**
     * Obtener tickets con filtros según rol del usuario
     */
    public Page<TicketFiltroDTO> obtenerTicketsConFiltros(
            Long usuarioId,
            String usuarioRol,
            Long pacienteId,
            Long terapeutaId,
            EstadoTicket estadoTicket,
            EstadoFinanciero estadoFinanciero,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        Page<Ticket> tickets;
        Role role = Role.fromNombre(usuarioRol);

        switch (role) {
            case PACIENTE:
                // Paciente solo puede ver tickets con sus citas
                tickets = ticketRepository.findByPacienteWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case TERAPEUTA:
                // Terapeuta solo puede ver tickets con sus citas
                tickets = ticketRepository.findByTerapeutaWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case ADMINISTRADOR:
                // Admin puede ver todos los tickets con filtros opcionales
                tickets = ticketRepository.findWithFilters(
                        pacienteId, terapeutaId, estadoTicket, estadoFinanciero,
                        fechaInicio, fechaFin, pageable);
                break;

            case ALUMNO:
            default:
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return tickets.map(TicketFiltroDTO::fromEntity);
    }

    /**
     * Obtener ticket por ID con validación de acceso
     */
    public TicketDTO obtenerTicketConSeguridad(Long ticketId, Long usuarioId, String usuarioRol) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Role role = Role.fromNombre(usuarioRol);

        // Validar acceso según rol
        switch (role) {
            case PACIENTE:
                if (ticket.getCita() == null ||
                    ticket.getCita().getPaciente() == null ||
                    !ticket.getCita().getPaciente().getUsuarioId().equals(usuarioId)) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            case TERAPEUTA:
                if (ticket.getCita() == null ||
                    ticket.getCita().getTerapeuta() == null ||
                    !ticket.getCita().getTerapeuta().getUsuarioId().equals(usuarioId)) {
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

    /**
     * Obtener tickets del paciente autenticado
     */
    public Page<TicketDTO> obtenerMisTickets(Long pacienteId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findTicketsByPaciente(pacienteId, pageable);
        return tickets.map(this::mapToDTO);
    }

    /**
     * Validar transiciones de estado permitidas
     */
    private void validarTransicionEstado(EstadoTicket estadoActual, EstadoTicket estadoNuevo) {
        if (estadoActual == EstadoTicket.CANCELADO || estadoActual == EstadoTicket.COMPLETADO) {
            throw new BusinessException("No se puede modificar un ticket en estado: " + estadoActual);
        }

        // Validaciones adicionales según estado actual
        switch (estadoActual) {
            case BORRADOR:
                // Desde BORRADOR se puede ir a cualquier estado
                break;
            case AGENDADO:
                if (estadoNuevo != EstadoTicket.EN_PROGRESO &&
                    estadoNuevo != EstadoTicket.COMPLETADO &&
                    estadoNuevo != EstadoTicket.CANCELADO) {
                    throw new BusinessException("Transición no permitida: " + estadoActual + " -> " + estadoNuevo);
                }
                break;
            case EN_PROGRESO:
                if (estadoNuevo != EstadoTicket.COMPLETADO && estadoNuevo != EstadoTicket.CANCELADO) {
                    throw new BusinessException("Transición no permitida: " + estadoActual + " -> " + estadoNuevo);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Mapear Ticket a TicketDTO
     */
    private TicketDTO mapToDTO(Ticket ticket) {
        return TicketDTO.fromEntity(ticket);
    }
}
