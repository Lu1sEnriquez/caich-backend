package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.ticket.TicketBasicoDTO;
import com.api_caich.dto.ticket.TicketCitaDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.dto.ticket.TicketEspacioDTO;
import com.api_caich.dto.ticket.TicketFiltroDTO;
import com.api_caich.dto.ticket.TicketPrestamoDTO;
import com.api_caich.dto.ticket.TicketProductosUpdateDTO;
import com.api_caich.dto.ticket.TicketRequestDTO;
import com.api_caich.dto.ticket.TicketVentaDTO;
import com.api_caich.entity.Espacio;
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
import com.api_caich.enums.MetodoPago;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.TicketCitaRepository;
import com.api_caich.repository.TicketEspacioRepository;
import com.api_caich.repository.TicketRepository;
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
    private final TicketCitaRepository ticketCitaRepository;
    private final TicketEspacioRepository ticketEspacioRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final FolioGenerator folioGenerator;
    private final MonederoService monederoService;

    /**
     * Crear ticket con Arquitectura Modular de Composición Soporta Ventas,
     * Citas, Espacios y Préstamos en un solo ticket
     */
    @Transactional
    public TicketDTO crearTicket(TicketRequestDTO request) {
        log.info("Creando ticket modular para usuario: {}", request.getCreadoPorId());

        // 1. Validar y obtener el usuario creador
        Long creadoPorId = Objects.requireNonNull(request.getCreadoPorId(), "creadoPorId requerido");
        Usuario creadoPor = usuarioRepository.findById(creadoPorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        Usuario comprador = null;
        if (request.getCompradorId() != null) {
            comprador = usuarioRepository.findById(request.getCompradorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comprador no encontrado"));
        }

        if (Boolean.TRUE.equals(request.getCobrarConMonedero())
                && (comprador == null || !Role.ALUMNO.getNombre().equalsIgnoreCase(comprador.getRol()))) {
            throw new BusinessException("El cobro con monedero solo está disponible para alumnos");
        }

        // 2. Generar folio único
        String folio = folioGenerator.generarFolioTicket();

        // 3. Crear la entidad Ticket principal
        Ticket ticket = new Ticket();
        ticket.setFolio(folio);
        ticket.setCreadoPor(creadoPor);
        ticket.setComprador(comprador);
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
                Long productoId = Objects.requireNonNull(ventaDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
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
                        "Venta en ticket: " + folio, "TICKET_VENTA_" + folio, creadoPorId);
            }
        }

        // 5. Procesar CITA (máximo 1 por ticket)
        if (request.getCita() != null) {
            TicketCitaDTO citaDTO = request.getCita();
            
            // Validar modalidad
            String modalidad = citaDTO.getModalidad() != null ? citaDTO.getModalidad() : "Presencial";
            if (!modalidad.equals("Presencial") && !modalidad.equals("Online")) {
                throw new BusinessException("Modalidad inválida. Debe ser 'Presencial' u 'Online'");
            }
            
            // Validar que citas presenciales requieran espacio
            if (modalidad.equals("Presencial") && (request.getEspacios() == null || request.getEspacios().isEmpty())) {
                throw new BusinessException("Las citas presenciales requieren asignar un cubículo");
            }
            
            Usuario paciente = null;
            if (citaDTO.getPacienteId() != null) {
                Long pacienteId = Objects.requireNonNull(citaDTO.getPacienteId(), "pacienteId requerido");
                paciente = usuarioRepository.findById(pacienteId)
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            }
            Usuario terapeuta = null;
            if (citaDTO.getTerapeutaId() != null) {
                Long terapeutaId = Objects.requireNonNull(citaDTO.getTerapeutaId(), "terapeutaId requerido");
                terapeuta = usuarioRepository.findById(terapeutaId)
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
            cita.setModalidad(modalidad);
            // Para citas Online, no asignar espacio
            if (modalidad.equals("Online")) {
                // Las citas online no necesitan espacio
            }
            ticket.setCita(cita);
        }

        // 6. Procesar ESPACIOS (Renta de cubículos)
        if (request.getEspacios() != null && !request.getEspacios().isEmpty()) {
            for (TicketEspacioDTO espacioDTO : request.getEspacios()) {
                Long espacioId = Objects.requireNonNull(espacioDTO.getEspacioId(), "espacioId requerido");
                Espacio espacio = espacioRepository.findById(espacioId)
                        .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado: " + espacioDTO.getEspacioId()));

                // Validar tiempos básicos
                if (espacioDTO.getHoraInicioReserva() != null && espacioDTO.getHoraFinReserva() != null) {
                    if (!espacioDTO.getHoraFinReserva().isAfter(espacioDTO.getHoraInicioReserva())) {
                        throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
                    }
                    
                    // Validar conflictos de reservas
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
                ticketEspacio.setCelular(espacioDTO.getCelular());
                ticketEspacio.setActividad(espacioDTO.getActividad());
                ticketEspacio.setMaestro(espacioDTO.getMaestro());
                ticket.getEspacios().add(ticketEspacio);
            }
        }

        // 7. Procesar PRÉSTAMOS
        if (request.getPrestamos() != null && !request.getPrestamos().isEmpty()) {
            for (TicketPrestamoDTO prestamoDTO : request.getPrestamos()) {
                Long productoId = Objects.requireNonNull(prestamoDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + prestamoDTO.getProductoId()));

                if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
                    throw new BusinessException("El producto no es prestable: " + producto.getNombre());
                }

                Long responsableId = Objects.requireNonNull(prestamoDTO.getResponsableId(), "responsableId requerido");
                Usuario responsable = usuarioRepository.findById(responsableId)
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

        // El cobro desde monedero es inmediato y deja el movimiento ligado al ticket.
        if (Boolean.TRUE.equals(request.getCobrarConMonedero())) {
            monederoService.descontarSaldo(
                    comprador.getUsuarioId(), ticketGuardado, ticketGuardado.getMontoTotal());
            ticketGuardado.setMontoPagado(ticketGuardado.getMontoTotal());
            ticketGuardado.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            ticketGuardado = ticketRepository.save(ticketGuardado);
        }

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
     * Actualizar ticket completo (reemplaza toda la información)
     * Permite actualizar cita, espacios, productos (ventas/préstamos)
     */
    @Transactional
    public TicketDTO actualizarTicket(Long ticketId, TicketRequestDTO request, Long usuarioId) {
        log.info("Actualizando ticket completo: {}", ticketId);

        // 1. Obtener el ticket existente
        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO) {
            throw new BusinessException("No se puede editar un ticket cancelado");
        }

        if (ticket.getEstadoTicket() == EstadoTicket.COMPLETADO) {
            throw new BusinessException("No se puede editar un ticket completado");
        }

        // 2. Actualizar estados si se proporcionan
        if (request.getEstadoTicket() != null && !request.getEstadoTicket().equals(ticket.getEstadoTicket())) {
            validarTransicionEstado(ticket.getEstadoTicket(), request.getEstadoTicket());
            ticket.setEstadoTicket(request.getEstadoTicket());
        }

        if (request.getEstadoFinanciero() != null) {
            ticket.setEstadoFinanciero(request.getEstadoFinanciero());
        }

        // 3. Actualizar costos adicionales
        ticket.setCostoAdicional(request.getCostoAdicional() != null ? request.getCostoAdicional() : BigDecimal.ZERO);
        ticket.setMotivoCostoAdicional(request.getMotivoCostoAdicional());

        // 4. ACTUALIZAR VENTAS - Devolver stock de ventas antiguas, crear nuevas
        if (ticket.getVentas() != null) {
            for (TicketVenta venta : ticket.getVentas()) {
                inventarioService.incrementarStock(
                        venta.getProducto().getProductoId(),
                        venta.getCantidad(),
                        "Devolución por actualización de ticket: " + ticket.getFolio(),
                        "TICKET_UPDATE_" + ticketId,
                        usuarioId
                );
            }
            ticket.getVentas().clear();
        }

        if (request.getVentas() != null && !request.getVentas().isEmpty()) {
            for (TicketVentaDTO ventaDTO : request.getVentas()) {
                Long productoId = Objects.requireNonNull(ventaDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + ventaDTO.getProductoId()));

                if (producto.getStock() < ventaDTO.getCantidad()) {
                    throw new BusinessException("Stock insuficiente para: " + producto.getNombre());
                }

                TicketVenta venta = new TicketVenta();
                venta.setTicket(ticket);
                venta.setProducto(producto);
                venta.setCantidad(ventaDTO.getCantidad());
                venta.setPrecioUnitario(ventaDTO.getPrecioUnitario());
                venta.setSubtotal(ventaDTO.getPrecioUnitario().multiply(BigDecimal.valueOf(ventaDTO.getCantidad())));
                ticket.getVentas().add(venta);

                inventarioService.reducirStock(producto.getProductoId(), ventaDTO.getCantidad(),
                        "Venta actualizada en ticket: " + ticket.getFolio(), "TICKET_VENTA_" + ticket.getFolio(), usuarioId);
            }
        }

        // 5. ACTUALIZAR CITA
        if (request.getCita() != null) {
            TicketCitaDTO citaDTO = request.getCita();
            String modalidad = citaDTO.getModalidad() != null ? citaDTO.getModalidad() : "Presencial";

            if (!modalidad.equals("Presencial") && !modalidad.equals("Online")) {
                throw new BusinessException("Modalidad inválida. Debe ser 'Presencial' u 'Online'");
            }

            if (modalidad.equals("Presencial") && (request.getEspacios() == null || request.getEspacios().isEmpty())) {
                throw new BusinessException("Las citas presenciales requieren asignar un cubículo");
            }

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

            // Validar conflictos
            if (paciente != null && citaDTO.getFechaInicio() != null && citaDTO.getFechaFin() != null) {
                Long conflictos = ticketCitaRepository.countConflictingCitasForPaciente(
                        paciente.getUsuarioId(),
                        citaDTO.getFechaInicio(),
                        citaDTO.getFechaFin(),
                        List.of(EstadoTicket.CANCELADO),
                        ticket.getCita() != null ? ticket.getCita().getId() : null
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
                        ticket.getCita() != null ? ticket.getCita().getId() : null
                );
                if (conflictos > 0) {
                    throw new BusinessException("El terapeuta ya tiene una cita en ese horario");
                }
            }

            if (ticket.getCita() == null) {
                ticket.setCita(new TicketCita());
                ticket.getCita().setTicket(ticket);
            }

            ticket.getCita().setPaciente(paciente);
            ticket.getCita().setTerapeuta(terapeuta);
            ticket.getCita().setFechaInicio(citaDTO.getFechaInicio());
            ticket.getCita().setFechaFin(citaDTO.getFechaFin());
            ticket.getCita().setPrecioCobrado(citaDTO.getPrecioCobrado());
            ticket.getCita().setNotasTerapia(citaDTO.getNotasTerapia());
            ticket.getCita().setModalidad(modalidad);
        } else {
            // Si no se proporciona cita, eliminar la existente
            ticket.setCita(null);
        }

        // 6. ACTUALIZAR ESPACIOS - Eliminar viejos, crear nuevos
        if (ticket.getEspacios() != null) {
            ticket.getEspacios().clear();
        }

        if (request.getEspacios() != null && !request.getEspacios().isEmpty()) {
            for (TicketEspacioDTO espacioDTO : request.getEspacios()) {
                Long espacioId = Objects.requireNonNull(espacioDTO.getEspacioId(), "espacioId requerido");
                Espacio espacio = espacioRepository.findById(espacioId)
                        .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

                LocalDateTime horaInicio = Objects.requireNonNull(espacioDTO.getHoraInicioReserva(), "horaInicioReserva requerida");
                LocalDateTime horaFin = Objects.requireNonNull(espacioDTO.getHoraFinReserva(), "horaFinReserva requerida");

                if (!horaFin.isAfter(horaInicio)) {
                    throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
                }

                Long conflictos = ticketEspacioRepository.countConflictingReservas(
                        espacio.getEspacioId(),
                        horaInicio,
                        horaFin,
                        List.of(EstadoTicket.CANCELADO),
                        null
                );

                if (conflictos > 0) {
                    throw new BusinessException("El espacio " + espacio.getNombre() + " ya está reservado en ese horario");
                }

                TicketEspacio reserva = new TicketEspacio();
                reserva.setTicket(ticket);
                reserva.setEspacio(espacio);
                reserva.setHoraInicioReserva(horaInicio);
                reserva.setHoraFinReserva(horaFin);
                reserva.setCostoCobrado(espacioDTO.getCostoCobrado());
                ticket.getEspacios().add(reserva);
            }
        }

        // 7. ACTUALIZAR PRÉSTAMOS - Marcar viejos como devueltos, crear nuevos
        if (ticket.getPrestamos() != null) {
            for (TicketPrestamo prestamo : ticket.getPrestamos()) {
                if (prestamo.getEstado() == EstadoPrestamo.ACTIVO) {
                    prestamo.setEstado(EstadoPrestamo.DEVUELTO);
                    prestamo.setFechaDevolucionReal(LocalDateTime.now());
                }
            }
        }

        if (request.getPrestamos() != null && !request.getPrestamos().isEmpty()) {
            for (TicketPrestamoDTO prestamoDTO : request.getPrestamos()) {
                Long productoId = Objects.requireNonNull(prestamoDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

                Long responsableId = Objects.requireNonNull(prestamoDTO.getResponsableId(), "responsableId requerido");
                Usuario responsable = usuarioRepository.findById(responsableId)
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

        // 8. Recalcular total
        ticket.recalcularTotal();

        // 9. Recalcular estado financiero basado en montoPagado vs montoTotal
        recalcularEstadoFinanciero(ticket);

        // 10. Guardar
        Ticket ticketActualizado = ticketRepository.save(ticket);

        // 11. Auditoría
        auditoriaService.registrarCambio("Tickets", ticketId, "Actualizar",
                "Ticket actualizado completamente", null, null, usuarioId, "SYSTEM");

        log.info("Ticket actualizado exitosamente: {}", ticketId);
        return mapToDTO(ticketActualizado);
    }

    /**
     * Actualizar estado del ticket
     */
    @Transactional
    public TicketDTO actualizarEstado(Long ticketId, EstadoTicket nuevoEstado, String notas, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
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

        Ticket ticketActualizado = ticketRepository.save(Objects.requireNonNull(ticket, "ticket requerido"));
        return mapToDTO(ticketActualizado);
    }

    /**
     * Actualizar reserva de espacio (renta de cubículo)
     */
    @Transactional
    public TicketDTO actualizarReservaEspacio(
            Long ticketId,
            Long reservaId,
            TicketEspacioDTO request,
            Long usuarioId) {

        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO || ticket.getEstadoTicket() == EstadoTicket.COMPLETADO) {
            throw new BusinessException("No se puede editar un ticket en estado: " + ticket.getEstadoTicket());
        }

        TicketEspacio reserva = ticketEspacioRepository.findById(Objects.requireNonNull(reservaId, "reservaId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de espacio no encontrada"));

        if (!reserva.getTicket().getTicketId().equals(ticketId)) {
            throw new BusinessException("La reserva no pertenece al ticket especificado");
        }

        Long espacioId = Objects.requireNonNull(request.getEspacioId(), "espacioId requerido");
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        LocalDateTime horaInicio = Objects.requireNonNull(request.getHoraInicioReserva(), "horaInicioReserva requerida");
        LocalDateTime horaFin = Objects.requireNonNull(request.getHoraFinReserva(), "horaFinReserva requerida");

        if (!horaFin.isAfter(horaInicio)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }

        // Validar conflictos básicos
        Long conflictos = ticketEspacioRepository.countConflictingReservas(
                espacio.getEspacioId(),
                horaInicio,
                horaFin,
                List.of(EstadoTicket.CANCELADO),
                reserva.getId());

        if (conflictos > 0) {
            throw new BusinessException("El espacio ya está reservado en ese horario");
        }

        reserva.setEspacio(espacio);
        reserva.setHoraInicioReserva(horaInicio);
        reserva.setHoraFinReserva(horaFin);
        reserva.setCostoCobrado(request.getCostoCobrado());
        ticketEspacioRepository.save(reserva);

        ticket.recalcularTotal();
        ticketRepository.save(ticket);

        auditoriaService.registrarCambio(
                "Ticket_Espacios",
                reserva.getId(),
                "Actualizar",
                "Reserva de espacio actualizada",
                null,
                null,
                usuarioId,
                "SYSTEM");

        return mapToDTO(ticket);
    }

    /**
     * Cancelar ticket y revertir operaciones (stock, préstamos)
     */
    @Transactional
    public void cancelarTicket(Long ticketId, String motivo, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
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

        // Si el ticket fue pagado con monedero, crear solicitud de devolución pendiente
        boolean fuePagadoConMonedero = ticket.getPagos() != null && ticket.getPagos().stream()
                .anyMatch(p -> p.getMetodoPago() == MetodoPago.MONEDERO
                            && p.getEstado() == EstadoPagoTransaccion.APROBADO);
        if (fuePagadoConMonedero) {
            monederoService.crearSolicitudDevolucion(ticket);
        }

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
     * Actualizar productos del ticket (ventas y préstamos)
     */
    @Transactional
    public TicketDTO actualizarProductos(Long ticketId, TicketProductosUpdateDTO request, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO || ticket.getEstadoTicket() == EstadoTicket.COMPLETADO) {
            throw new BusinessException("No se puede editar un ticket en estado: " + ticket.getEstadoTicket());
        }

        // Revertir ventas actuales (stock)
        if (ticket.getVentas() != null) {
            for (TicketVenta venta : ticket.getVentas()) {
                inventarioService.incrementarStock(
                        venta.getProducto().getProductoId(),
                        venta.getCantidad(),
                        "Reversion de venta en ticket: " + ticket.getFolio(),
                        "TICKET_VENTA_UPD_" + ticket.getTicketId(),
                        usuarioId
                );
            }
            ticket.getVentas().clear();
        }

        // Limpiar préstamos actuales
        if (ticket.getPrestamos() != null) {
            ticket.getPrestamos().clear();
        }

        // Agregar nuevas ventas
        if (request.getVentas() != null) {
            for (TicketVentaDTO ventaDTO : request.getVentas()) {
                Long productoId = Objects.requireNonNull(ventaDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

                if (producto.getStock() < ventaDTO.getCantidad()) {
                    throw new BusinessException("Stock insuficiente para: " + producto.getNombre()
                            + ". Disponible: " + producto.getStock()
                            + ", Solicitado: " + ventaDTO.getCantidad());
                }

                BigDecimal precioUnitario = ventaDTO.getPrecioUnitario() != null
                        ? ventaDTO.getPrecioUnitario()
                        : producto.getPrecio();

                if (precioUnitario == null) {
                    throw new BusinessException("Precio unitario requerido para el producto: " + producto.getNombre());
                }

                TicketVenta venta = new TicketVenta();
                venta.setTicket(ticket);
                venta.setProducto(producto);
                venta.setCantidad(ventaDTO.getCantidad());
                venta.setPrecioUnitario(precioUnitario);
                venta.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(ventaDTO.getCantidad())));
                ticket.getVentas().add(venta);

                inventarioService.reducirStock(
                        producto.getProductoId(),
                        ventaDTO.getCantidad(),
                        "Venta actualizada en ticket: " + ticket.getFolio(),
                        "TICKET_VENTA_UPD_" + ticket.getTicketId(),
                        usuarioId
                );
            }
        }

        // Agregar nuevos préstamos
        if (request.getPrestamos() != null) {
            for (TicketPrestamoDTO prestamoDTO : request.getPrestamos()) {
                Long productoId = Objects.requireNonNull(prestamoDTO.getProductoId(), "productoId requerido");
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

                if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
                    throw new BusinessException("El producto no es prestable: " + producto.getNombre());
                }

                Long responsableId = Objects.requireNonNull(prestamoDTO.getResponsableId(), "responsableId requerido");
                Usuario responsable = usuarioRepository.findById(responsableId)
                        .orElseThrow(() -> new ResourceNotFoundException("Responsable no encontrado"));

                TicketPrestamo prestamo = new TicketPrestamo();
                prestamo.setTicket(ticket);
                prestamo.setProducto(producto);
                prestamo.setResponsable(responsable);
                prestamo.setFechaPrestamo(prestamoDTO.getFechaPrestamo());
                prestamo.setFechaDevolucionEstimada(prestamoDTO.getFechaDevolucionEstimada());
                ticket.getPrestamos().add(prestamo);
            }
        }

        ticket.recalcularTotal();
        Ticket ticketActualizado = ticketRepository.save(ticket);

        auditoriaService.registrarCambio(
                "Tickets",
                ticketId,
                "Actualizar",
                "Productos del ticket actualizados",
                null,
                null,
                usuarioId,
                "SYSTEM");

        return mapToDTO(ticketActualizado);
    }

    /**
     * Obtener tickets con filtros según rol del usuario (RESPUESTA BÁSICA para listados)
     */
    public Page<TicketBasicoDTO> obtenerTicketsBasicos(
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

            case ALUMNO:
                // Alumno solo puede ver tickets (reservas cubículos) que creó
                tickets = ticketRepository.findByAlumnoWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case ADMINISTRADOR:
                // Admin puede ver todos los tickets con filtros opcionales
                tickets = ticketRepository.findWithFilters(
                        pacienteId, terapeutaId, estadoTicket, estadoFinanciero,
                        fechaInicio, fechaFin, pageable);
                break;

            default:
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return tickets.map(TicketBasicoDTO::fromEntity);
    }

    /**
     * Obtener tickets con filtros según rol del usuario (RESPUESTA DETALLADA - se mantiene por compatibilidad)
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

            case ALUMNO:
                // Alumno solo puede ver tickets (reservas cubículos) que creó
                tickets = ticketRepository.findByAlumnoWithFilters(
                        usuarioId, estadoTicket, fechaInicio, fechaFin, pageable);
                break;

            case ADMINISTRADOR:
                // Admin puede ver todos los tickets con filtros opcionales
                tickets = ticketRepository.findWithFilters(
                        pacienteId, terapeutaId, estadoTicket, estadoFinanciero,
                        fechaInicio, fechaFin, pageable);
                break;

            default:
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return tickets.map(TicketFiltroDTO::fromEntity);
    }

    /**
     * Obtener ticket por ID con validación de acceso
     */
    public TicketDTO obtenerTicketConSeguridad(Long ticketId, Long usuarioId, String usuarioRol) {
        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Role role = Role.fromNombre(usuarioRol);

        // Validar acceso según rol
        switch (role) {
            case PACIENTE:
                // Permitir acceso si es paciente de la cita, o si creó el ticket (préstamos)
                boolean esCreador = ticket.getCreadoPor() != null 
                    && ticket.getCreadoPor().getUsuarioId().equals(usuarioId);
                boolean esPacienteDeCita = ticket.getCita() != null
                        && ticket.getCita().getPaciente() != null
                        && ticket.getCita().getPaciente().getUsuarioId().equals(usuarioId);
                
                if (!esCreador && !esPacienteDeCita) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            case TERAPEUTA:
                // Permitir acceso si es terapeuta de la cita, o si creó el ticket (préstamos)
                boolean esCreadorTerapeuta = ticket.getCreadoPor() != null 
                    && ticket.getCreadoPor().getUsuarioId().equals(usuarioId);
                boolean esTerapeutaDeCita = ticket.getCita() != null
                        && ticket.getCita().getTerapeuta() != null
                        && ticket.getCita().getTerapeuta().getUsuarioId().equals(usuarioId);
                
                if (!esCreadorTerapeuta && !esTerapeutaDeCita) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            case ADMINISTRADOR:
                // Admin tiene acceso a todos
                break;

            case ALUMNO:
                // El alumno solo puede consultar sus propias reservas y compras.
                boolean esCreadorAlumno = ticket.getCreadoPor() != null
                        && ticket.getCreadoPor().getUsuarioId().equals(usuarioId);
                if (!esCreadorAlumno) {
                    throw new BusinessException("No tiene acceso a este ticket");
                }
                break;

            default:
                throw new BusinessException("No tiene permisos para ver tickets");
        }

        return mapToDTO(ticket);
    }

    /**
     * Obtener tickets del paciente autenticado
     */
    public Page<TicketDTO> obtenerMisTickets(Long pacienteId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findTicketsByPaciente(
                Objects.requireNonNull(pacienteId, "pacienteId requerido"),
                Objects.requireNonNull(pageable, "pageable requerido"));
        return tickets.map(this::mapToDTO);
    }

    /**
     * Recalcular totales del ticket y estado financiero.
     * Útil después de modificar productos, espacios o costos adicionales.
     */
    @Transactional
    public TicketDTO recalcularTotalesTicket(Long ticketId, Long usuarioId) {
        log.info("Recalculando totales para ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO) {
            throw new BusinessException("No se puede recalcular un ticket cancelado");
        }

        // Recalcular monto total
        ticket.recalcularTotal();

        // Recalcular estado financiero basado en montoPagado vs montoTotal
        recalcularEstadoFinanciero(ticket);

        // Guardar cambios
        Ticket ticketActualizado = ticketRepository.save(ticket);

        // Auditoría
        auditoriaService.registrarCambio("Tickets", ticketId, "RecalcularTotales",
                "Totales recalculados manualmente", null, null, usuarioId, "SYSTEM");

        log.info("Totales recalculados para ticket {}: montoTotal={}, montoPagado={}, saldoPendiente={}, estadoFinanciero={}",
                ticketId, ticket.getMontoTotal(), ticket.getMontoPagado(), 
                ticket.getSaldoPendiente(), ticket.getEstadoFinanciero());

        return mapToDTO(ticketActualizado);
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
            case BORRADOR -> {
                // Desde BORRADOR se puede ir a cualquier estado
            }
            case AGENDADO -> {
                if (estadoNuevo != EstadoTicket.EN_PROGRESO
                        && estadoNuevo != EstadoTicket.COMPLETADO
                        && estadoNuevo != EstadoTicket.CANCELADO) {
                    throw new BusinessException("Transición no permitida: " + estadoActual + " -> " + estadoNuevo);
                }
            }
            case EN_PROGRESO -> {
                if (estadoNuevo != EstadoTicket.COMPLETADO && estadoNuevo != EstadoTicket.CANCELADO) {
                    throw new BusinessException("Transición no permitida: " + estadoActual + " -> " + estadoNuevo);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Recalcular estado financiero basado en montoPagado vs montoTotal.
     * REGLA INMUTABLE: montoPagado = suma de pagos APROBADOS únicamente.
     * 
     * Estados financieros:
     * - PENDIENTE: montoPagado = 0
     * - PAGO_PARCIAL: 0 < montoPagado < montoTotal
     * - PAGADO: montoPagado >= montoTotal (incluye sobrepagos)
     * - EN_REVISION: hay pagos pendientes de verificación
     */
    private void recalcularEstadoFinanciero(Ticket ticket) {
        BigDecimal montoPagado = ticket.getMontoPagado();
        BigDecimal montoTotal = ticket.getMontoTotal();

        // Si hay pagos en revisión, mantener ese estado
        boolean tienePagosEnRevision = ticket.getPagos().stream()
                .anyMatch(pago -> pago.getEstado() == EstadoPagoTransaccion.PENDIENTE_REVISION);

        if (tienePagosEnRevision && montoPagado.compareTo(BigDecimal.ZERO) == 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.EN_REVISION);
            return;
        }

        // Calcular estado basado en montos
        if (montoPagado.compareTo(BigDecimal.ZERO) == 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        } else if (montoPagado.compareTo(montoTotal) >= 0) {
            // Incluye sobrepagos (montoPagado > montoTotal)
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        } else {
            // 0 < montoPagado < montoTotal
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
        }
    }

    /**
     * Mapear Ticket a TicketDTO
     */
    private TicketDTO mapToDTO(Ticket ticket) {
        return TicketDTO.fromEntity(ticket);
    }
}
