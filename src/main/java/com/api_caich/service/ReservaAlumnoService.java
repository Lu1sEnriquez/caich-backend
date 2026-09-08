package com.api_caich.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.horario.DisponibilidadDTO;
import com.api_caich.dto.reserva.CrearReservaAlumnoDTO;
import com.api_caich.dto.reserva.MisReservasDTO;
import com.api_caich.dto.reserva.ReservaAlumnoResponseDTO;
import com.api_caich.dto.reserva.ReprogramarReservaAlumnoDTO;
import com.api_caich.dto.reserva.ReprogramarReservaAlumnoResponseDTO;
import com.api_caich.entity.Espacio;
import com.api_caich.entity.Pago;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Sancion;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.TicketEspacio;
import com.api_caich.entity.TicketPrestamo;
import com.api_caich.entity.TicketVenta;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.EstadoPrestamo;
import com.api_caich.enums.EstadoSancion;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.enums.EstadoUsuario;
import com.api_caich.enums.Role;
import com.api_caich.enums.TipoSancion;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.SancionRepository;
import com.api_caich.repository.TicketEspacioRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;
import com.api_caich.util.FolioGenerator;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.api_caich.service.MonederoService;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaAlumnoService {

    private final TicketRepository ticketRepository;
    private final TicketEspacioRepository ticketEspacioRepository;
    private final SancionRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ProductoRepository productoRepository;
    private final DisponibilidadService disponibilidadService;
    private final NotificacionService notificacionService;
    private final InventarioService inventarioService;
    private final FolioGenerator folioGenerator;
    private final MonederoService monederoService;
    private final TicketService ticketService;
    private final PasswordEncoder passwordEncoder;
    /**
     * Obtiene las reservas del alumno
     */
    @Transactional(readOnly = true)
    public List<MisReservasDTO> obtenerMisReservas(Long alumnoId, String estado, LocalDate fecha) {
        log.info("Obteniendo reservas para alumno ID: {}", alumnoId);

        List<Ticket> tickets = ticketRepository.findByCreadoPorUsuarioId(alumnoId);

        return tickets.stream()
                .filter(t -> !t.getEspacios().isEmpty()) // Solo tickets con espacios
                .filter(t -> {
                    if (estado != null) {
                        return t.getEstadoTicket().name().equals(estado);
                    }
                    return true;
                })
                .flatMap(t -> t.getEspacios().stream().map(te -> {
                    Pago pago = t.getPagos().isEmpty() ? null : t.getPagos().get(0);
                    
                    LocalDate fechaReserva = te.getHoraInicioReserva().toLocalDate();
                    if (fecha != null && !fechaReserva.equals(fecha)) {
                        return null;
                    }

                    return MisReservasDTO.builder()
                            .ticketId(t.getTicketId())
                            .espacioReservaId(te.getId())
                            .espacioId(te.getEspacio().getEspacioId())
                            .folio(t.getFolio())
                            .espacioNombre(te.getEspacio().getNombre())
                            .fecha(fechaReserva)
                            .horaInicio(te.getHoraInicioReserva().toLocalTime())
                            .horaFin(te.getHoraFinReserva().toLocalTime())
                            .costo(te.getCostoCobrado())
                            .estadoTicket(t.getEstadoTicket().name())
                            .estadoPago(pago != null ? pago.getEstado().name() : "SIN_PAGO")
                            .urlComprobante(pago != null ? pago.getComprobanteUrl() : null)
                            .estadoVerificacionComprobante(pago != null ? pago.getEstado().name() : null)
                            .motivoRechazo(pago != null ? pago.getMotivoRechazo() : null)
                            .celular(te.getCelular())
                            .actividad(te.getActividad())
                            .maestro(te.getMaestro())
                            .idAlumno(t.getCreadoPor().getIdAlumno())
                            .build();
                }))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Reprograma una reserva propia respetando las reglas normales del horario.
     * Si cambia la duración, ajusta el monedero en la misma transacción: cobra la
     * diferencia si aumenta el costo y acredita el saldo si disminuye.
     */
    @Transactional
    public ReprogramarReservaAlumnoResponseDTO reprogramarReserva(
            Long ticketId,
            ReprogramarReservaAlumnoDTO request,
            Long alumnoId) {

        Ticket ticket = obtenerTicketPropioActivo(ticketId, alumnoId);
        TicketEspacio reserva = obtenerReservaUnica(ticket);
        validarAnticipacionDeGestion(reserva);

        LocalDateTime inicio = LocalDateTime.of(request.getFecha(), request.getHoraInicio());
        LocalDateTime fin = LocalDateTime.of(request.getFecha(), request.getHoraFin());
        if (!fin.isAfter(inicio)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La nueva reserva debe iniciar en una fecha y hora futuras");
        }

        long duracionNueva = java.time.Duration.between(inicio, fin).toMinutes();
        validarDuracionConfigurada(reserva.getEspacio().getEspacioId(), inicio.toLocalDate(), duracionNueva, ticketId);

        if (!disponibilidadService.estaDisponible(
                reserva.getEspacio().getEspacioId(), inicio, fin, ticketId)) {
            throw new BusinessException("El horario seleccionado ya no está disponible");
        }

        BigDecimal costoAnterior = reserva.getCostoCobrado();
        BigDecimal nuevoCosto = calcularCostoReserva(reserva.getEspacio(), duracionNueva);
        BigDecimal diferencia = nuevoCosto.subtract(costoAnterior);

        // Primero se valida y ajusta el saldo; cualquier error revierte también
        // el cambio de horario gracias a la transacción.
        if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
            Pago pago = monederoService.descontarSaldo(alumnoId, ticket, diferencia);
            ticket.getPagos().add(pago);
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            Pago pago = monederoService.acreditarAjusteReprogramacion(alumnoId, ticket, diferencia.abs());
            ticket.getPagos().add(pago);
        }

        reserva.setHoraInicioReserva(inicio);
        reserva.setHoraFinReserva(fin);
        reserva.setCostoCobrado(nuevoCosto);
        ticketEspacioRepository.save(reserva);

        ticket.recalcularTotal();
        recalcularPagoYEstadoFinanciero(ticket);
        ticketRepository.save(ticket);

        log.info("Reserva {} reprogramada por alumno {}. Costo {} -> {}", ticketId, alumnoId, costoAnterior, nuevoCosto);
        return ReprogramarReservaAlumnoResponseDTO.builder()
                .nuevoCosto(nuevoCosto)
                .diferencia(diferencia)
                .build();
    }

    /** Cancela una reserva propia usando el mismo flujo de devolución de monedero. */
    @Transactional
    public void cancelarReserva(Long ticketId, String motivo, Long alumnoId) {
        Ticket ticket = obtenerTicketPropioActivo(ticketId, alumnoId);
        validarAnticipacionDeGestion(obtenerReservaUnica(ticket));
        ticketService.cancelarTicket(ticketId, motivo, alumnoId);
    }

    private Ticket obtenerTicketPropioActivo(Long ticketId, Long alumnoId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (ticket.getCreadoPor() == null
                || !ticket.getCreadoPor().getUsuarioId().equals(alumnoId)) {
            throw new BusinessException("No tienes permiso para gestionar esta reserva");
        }
        if (ticket.getEstadoTicket() == EstadoTicket.CANCELADO
                || ticket.getEstadoTicket() == EstadoTicket.COMPLETADO
                || ticket.getEstadoTicket() == EstadoTicket.NO_ASISTIO) {
            throw new BusinessException("No se puede gestionar una reserva en estado: " + ticket.getEstadoTicket());
        }
        return ticket;
    }

    private TicketEspacio obtenerReservaUnica(Ticket ticket) {
        if (ticket.getEspacios() == null || ticket.getEspacios().size() != 1) {
            throw new BusinessException("Esta operación solo está disponible para reservas de un cubículo");
        }
        return ticket.getEspacios().get(0);
    }

    /** El alumno puede gestionar por su cuenta solo con más de 24 h de anticipación. */
    private void validarAnticipacionDeGestion(TicketEspacio reserva) {
        LocalDateTime limite = reserva.getHoraInicioReserva().minusHours(24);
        if (!LocalDateTime.now().isBefore(limite)) {
            throw new BusinessException(
                    "Solo puedes cancelar o reprogramar hasta 24 horas antes del inicio de la reserva");
        }
    }

    private void validarDuracionConfigurada(
            Long espacioId, LocalDate fecha, long duracionMinutos, Long ticketId) {
        DisponibilidadDTO disponibilidad = disponibilidadService.obtenerSlotsDisponibles(espacioId, fecha, ticketId);
        if (!Boolean.TRUE.equals(disponibilidad.getAbierto()) || disponibilidad.getHorarioBase() == null) {
            throw new BusinessException("El espacio no está disponible en la fecha seleccionada");
        }
        int minima = disponibilidad.getHorarioBase().getDuracionMinimaMinutos();
        int maxima = disponibilidad.getHorarioBase().getDuracionMaximaMinutos();
        if (duracionMinutos < minima || duracionMinutos > maxima) {
            throw new BusinessException("La duración debe estar entre " + minima + " y " + maxima + " minutos");
        }
    }

    private BigDecimal calcularCostoReserva(Espacio espacio, long duracionMinutos) {
        return espacio.getCostoPorHora()
                .multiply(BigDecimal.valueOf(duracionMinutos))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private void recalcularPagoYEstadoFinanciero(Ticket ticket) {
        BigDecimal montoPagado = ticket.getPagos().stream()
                .filter(pago -> pago.getEstado() == EstadoPagoTransaccion.APROBADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ticket.setMontoPagado(montoPagado);
        if (montoPagado.compareTo(BigDecimal.ZERO) <= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        } else if (montoPagado.compareTo(ticket.getMontoTotal()) >= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        } else {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
        }
    }

    /**
     * Obtiene disponibilidad de un espacio para una fecha
     */
    @Transactional(readOnly = true)
    public DisponibilidadDTO obtenerDisponibilidad(Long espacioId, LocalDate fecha) {
        log.info("Obteniendo disponibilidad para espacio ID: {} en fecha: {}", espacioId, fecha);

        // Validar que el espacio existe
        espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        // Delegar al servicio de disponibilidad
        return disponibilidadService.obtenerSlotsDisponibles(espacioId, fecha);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NUEVO FLUJO: RESERVA CON MONEDERO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea una reserva de cubículo y descuenta el monto directamente del monedero
     * del alumno. Si el saldo es suficiente, la reserva queda AGENDADA y el pago
     * queda APROBADO de forma inmediata (sin revisión manual del admin).
     */
    @Transactional
    public ReservaAlumnoResponseDTO crearReservaConMonedero(
            CrearReservaAlumnoDTO request,
            Long alumnoId) {

        log.info("Creando reserva con monedero para alumno ID: {}", alumnoId);

        // 1. Validar usuario
        Usuario alumno = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (!alumno.getRol().equalsIgnoreCase(Role.ALUMNO.getNombre())) {
            throw new BusinessException("El usuario no es un alumno");
        }

        // 2. Verificar sanciones activas
        List<Sancion> sancionesActivas = sancionRepository.findSancionesActivasParaAlumno(
                alumnoId, EstadoSancion.ACTIVA, LocalDate.now());

        boolean tieneSuspension = sancionesActivas.stream()
                .anyMatch(s -> s.getTipoSancion() == TipoSancion.SUSPENSION ||
                              s.getTipoSancion() == TipoSancion.SUSPENSION_PERMANENTE);

        if (tieneSuspension) {
            throw new BusinessException("No puede realizar reservas debido a una sanción activa");
        }

        // 3. Validar espacio
        Espacio espacio = espacioRepository.findById(request.getEspacioId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        // 4. Validar disponibilidad
        LocalDateTime horaInicio = LocalDateTime.of(request.getFecha(), request.getHoraInicio());
        LocalDateTime horaFin    = LocalDateTime.of(request.getFecha(), request.getHoraFin());

        if (!disponibilidadService.estaDisponible(request.getEspacioId(), horaInicio, horaFin)) {
            throw new BusinessException("El horario seleccionado no está disponible");
        }

        // 5. Calcular costo
        long duracionMinutos = java.time.Duration.between(horaInicio, horaFin).toMinutes();
        if (duracionMinutos <= 0) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }
        validarDuracionConfigurada(request.getEspacioId(), request.getFecha(), duracionMinutos, null);
        BigDecimal costo = calcularCostoReserva(espacio, duracionMinutos);

        // 6. Verificar saldo suficiente ANTES de crear el ticket
        //    (el descuento atómico real lo hace MonederoService.descontarSaldo)
        com.api_caich.entity.Monedero monedero = monederoService.obtenerOCrearMonedero(alumnoId);
        if (monedero.getSaldo().compareTo(costo) < 0) {
            throw new BusinessException(
                    "Saldo insuficiente en monedero. Disponible: $" + monedero.getSaldo() +
                    " | Requerido: $" + costo +
                    ". Recargue su monedero para continuar.");
        }

        // 7. Crear Ticket
        String folio = folioGenerator.generarFolioTicket();
        Ticket ticket = new Ticket();
        ticket.setFolio(folio);
        ticket.setCreadoPor(alumno);
        ticket.setEstadoTicket(EstadoTicket.AGENDADO);
        ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        ticket.setCostoAdicional(BigDecimal.ZERO);
        ticket.setMontoPagado(BigDecimal.ZERO);
        ticket.setVentas(new ArrayList<>());
        ticket.setEspacios(new ArrayList<>());
        ticket.setPrestamos(new ArrayList<>());
        ticket.setPagos(new ArrayList<>());
        ticket = ticketRepository.save(ticket);

        // 8. TicketEspacio
        TicketEspacio ticketEspacio = new TicketEspacio();
        ticketEspacio.setTicket(ticket);
        ticketEspacio.setEspacio(espacio);
        ticketEspacio.setHoraInicioReserva(horaInicio);
        ticketEspacio.setHoraFinReserva(horaFin);
        ticketEspacio.setCostoCobrado(costo);
        ticketEspacio.setCelular(request.getCelular());
        ticketEspacio.setActividad(request.getActividad());
        ticketEspacio.setMaestro(request.getMaestro());
        ticketEspacioRepository.save(ticketEspacio);
        ticket.getEspacios().add(ticketEspacio);

        // 8a. Vincular paciente a la reserva (seleccionado o nuevo con deduplicación)
        resolverPacienteParaEspacio(ticketEspacio, request.getPacienteId(), request.getPacienteNuevo());
        ticketEspacioRepository.save(ticketEspacio);

        // 8b. Ventas
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            for (CrearReservaAlumnoDTO.ProductoLineaDTO lineaDTO : request.getProductos()) {
                Producto producto = productoRepository.findById(lineaDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + lineaDTO.getProductoId()));
                if (!Boolean.TRUE.equals(producto.getEsVendible())) {
                    throw new BusinessException("El producto no está disponible para venta: " + producto.getNombre());
                }
                if (producto.getStock() < lineaDTO.getCantidad()) {
                    throw new BusinessException("Stock insuficiente para: " + producto.getNombre());
                }
                TicketVenta venta = new TicketVenta();
                venta.setTicket(ticket);
                venta.setProducto(producto);
                venta.setCantidad(lineaDTO.getCantidad());
                venta.setPrecioUnitario(producto.getPrecio());
                venta.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(lineaDTO.getCantidad())));
                ticket.getVentas().add(venta);
                inventarioService.reducirStock(producto.getProductoId(), lineaDTO.getCantidad(),
                        "Compra alumno en reserva con monedero: " + folio, "RESERVA_MONEDERO_" + folio, alumnoId);
            }
        }

        // 8c. Préstamos
        if (request.getPrestamos() != null && !request.getPrestamos().isEmpty()) {
            for (CrearReservaAlumnoDTO.PrestamoLineaDTO lineaDTO : request.getPrestamos()) {
                Producto producto = productoRepository.findById(lineaDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + lineaDTO.getProductoId()));
                if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
                    throw new BusinessException("El producto no es prestable: " + producto.getNombre());
                }
                TicketPrestamo prestamo = new TicketPrestamo();
                prestamo.setTicket(ticket);
                prestamo.setProducto(producto);
                prestamo.setResponsable(alumno);
                prestamo.setFechaPrestamo(horaInicio);
                prestamo.setFechaDevolucionEstimada(horaFin);
                prestamo.setEstado(EstadoPrestamo.ACTIVO);
                prestamo.setCostoUnitario(producto.getPrecio());
                ticket.getPrestamos().add(prestamo);
            }
        }

        // 9. Recalcular total
        ticket.recalcularTotal();
        ticketRepository.save(ticket);

        // 10. Descontar del monedero (operación atómica con optimistic lock)
        //     Esto también crea el registro de Pago con estado APROBADO
        com.api_caich.entity.Pago pago = monederoService.descontarSaldo(alumnoId, ticket, ticket.getMontoTotal());
        ticket.getPagos().add(pago);

        // 11. Marcar ticket como PAGADO
        ticket.setMontoPagado(ticket.getMontoTotal());
        ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        ticketRepository.save(ticket);

        // 12. Notificación
        notificacionService.notificarReservaAlumnoCreada(
                alumnoId, ticket.getTicketId(), folio, espacio.getNombre());

        log.info("Reserva con monedero confirmada: Ticket {}, Espacio {}", folio, espacio.getNombre());

        return ReservaAlumnoResponseDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(folio)
                .estado(ticket.getEstadoTicket().name())
                .mensaje("Reserva confirmada. El pago fue procesado desde tu monedero.")
                .build();
    }

    // =========================================================================
    // MÉTODO AUXILIAR: PACIENTE
    // =========================================================================

    /**
     * Vincula un paciente al espacio de un ticket.
     * Prioridad: pacienteId existente → buscar por nombre → crear nuevo.
     * La deduplicación es por nombre completo (sin distinción de mayúsculas).
     */
    private void resolverPacienteParaEspacio(
            TicketEspacio ticketEspacio,
            Long pacienteId,
            CrearReservaAlumnoDTO.PacienteNuevoDTO pacienteNuevo) {

        if (pacienteId != null) {
            Usuario paciente = usuarioRepository.findById(pacienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + pacienteId));
            ticketEspacio.setPaciente(paciente);
            return;
        }

        if (pacienteNuevo != null
                && pacienteNuevo.getNombreCompleto() != null
                && !pacienteNuevo.getNombreCompleto().isBlank()) {

            // Buscar primero por nombre exacto (deduplicación)
            Usuario paciente = usuarioRepository
                    .findByNombreCompletoIgnoreCaseAndRol(
                            pacienteNuevo.getNombreCompleto().trim(),
                            Role.PACIENTE.getNombre())
                    .orElseGet(() -> {
                        // No existe → crear nuevo paciente
                        Usuario nuevo = new Usuario();
                        nuevo.setNombreCompleto(pacienteNuevo.getNombreCompleto().trim());
                        nuevo.setTelefono(pacienteNuevo.getTelefono());
                        nuevo.setRol(Role.PACIENTE.getNombre());
                        nuevo.setEstado(EstadoUsuario.ACTIVO.getValor());
                        // Contraseña temporal no funcional (el paciente no inicia sesión directamente)
                        nuevo.setPasswordHash(passwordEncoder.encode(
                                java.util.UUID.randomUUID().toString()));
                        return usuarioRepository.save(nuevo);
                    });

            ticketEspacio.setPaciente(paciente);
        }
    }
}
