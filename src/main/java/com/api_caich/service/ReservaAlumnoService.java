package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.dto.horario.DisponibilidadDTO;
import com.api_caich.dto.reserva.CrearReservaAlumnoDTO;
import com.api_caich.dto.reserva.MisReservasDTO;
import com.api_caich.dto.reserva.ReservaAlumnoResponseDTO;
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
import com.api_caich.enums.MetodoPago;
import com.api_caich.enums.Role;
import com.api_caich.enums.TipoSancion;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.PagoRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.SancionRepository;
import com.api_caich.repository.TicketEspacioRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;
import com.api_caich.util.FolioGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaAlumnoService {

    private final TicketRepository ticketRepository;
    private final TicketEspacioRepository ticketEspacioRepository;
    private final PagoRepository pagoRepository;
    private final SancionRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ProductoRepository productoRepository;
    private final FileStorageService fileStorageService;
    private final DisponibilidadService disponibilidadService;
    private final NotificacionService notificacionService;
    private final InventarioService inventarioService;
    private final FolioGenerator folioGenerator;
    /**
     * Crea una reserva de cubículo para un alumno con su comprobante de pago
     */
    @Transactional
    public ReservaAlumnoResponseDTO crearReservaConPago(
            CrearReservaAlumnoDTO request,
            MultipartFile comprobante,
            Long alumnoId) {

        log.info("Creando reserva para alumno ID: {}", alumnoId);

        // 1. Validar usuario
        Usuario alumno = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (!alumno.getRol().equalsIgnoreCase(Role.ALUMNO.getNombre())) {
            throw new BusinessException("El usuario no es un alumno");
        }

        // 2. Verificar que el alumno no tenga sanciones activas
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
        LocalDateTime horaFin = LocalDateTime.of(request.getFecha(), request.getHoraFin());

        boolean disponible = disponibilidadService.estaDisponible(
                request.getEspacioId(),
                horaInicio,
                horaFin);

        if (!disponible) {
            throw new BusinessException("El horario seleccionado no está disponible");
        }

        // 5. Calcular costo
        long horas = ChronoUnit.HOURS.between(horaInicio, horaFin);
        if (horas <= 0) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }
        BigDecimal costo = espacio.getCostoPorHora().multiply(BigDecimal.valueOf(horas));

        // 6. Generar folio
        String folio = folioGenerator.generarFolioTicket();

        // 7. Crear Ticket
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

        // 8. Crear TicketEspacio
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

        // 8b. Procesar VENTAS (Productos compra alumno)
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            for (CrearReservaAlumnoDTO.ProductoLineaDTO lineaDTO : request.getProductos()) {
                Producto producto = productoRepository.findById(lineaDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Producto no encontrado: " + lineaDTO.getProductoId()));

                if (!Boolean.TRUE.equals(producto.getEsVendible())) {
                    throw new BusinessException(
                            "El producto no está disponible para venta: " + producto.getNombre());
                }

                if (producto.getStock() < lineaDTO.getCantidad()) {
                    throw new BusinessException("Stock insuficiente para: " + producto.getNombre()
                            + ". Disponible: " + producto.getStock()
                            + ", Solicitado: " + lineaDTO.getCantidad());
                }

                TicketVenta venta = new TicketVenta();
                venta.setTicket(ticket);
                venta.setProducto(producto);
                venta.setCantidad(lineaDTO.getCantidad());
                venta.setPrecioUnitario(producto.getPrecio());
                venta.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(lineaDTO.getCantidad())));
                ticket.getVentas().add(venta);

                inventarioService.reducirStock(producto.getProductoId(), lineaDTO.getCantidad(),
                        "Compra alumno en reserva: " + folio, "RESERVA_ALUMNO_" + folio, alumnoId);
            }
        }

        // 8c. Procesar PRÉSTAMOS (Alumno)
        if (request.getPrestamos() != null && !request.getPrestamos().isEmpty()) {
            for (CrearReservaAlumnoDTO.PrestamoLineaDTO lineaDTO : request.getPrestamos()) {
                Producto producto = productoRepository.findById(lineaDTO.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Producto no encontrado: " + lineaDTO.getProductoId()));

                if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
                    throw new BusinessException(
                            "El producto no es prestable: " + producto.getNombre());
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

        // 8d. Recalcular total del ticket
        ticket.recalcularTotal();
        ticketRepository.save(ticket);

        // 9. Guardar comprobante de pago
        String comprobanteUrl = fileStorageService.almacenarComprobante(comprobante, ticket.getTicketId());

        // 10. Crear registro de pago PENDIENTE_REVISION
        Pago pago = new Pago();
        pago.setTicket(ticket);
        pago.setMonto(ticket.getMontoTotal() != null ? ticket.getMontoTotal() : costo);
        pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
        pago.setComprobanteUrl(comprobanteUrl);
        pago.setEstado(EstadoPagoTransaccion.PENDIENTE_REVISION);
        pago.setNotas("Pago subido por alumno en reserva");
        pago.setFechaPago(LocalDateTime.now());

        pagoRepository.save(pago);
        ticket.getPagos().add(pago);

        // 11. Enviar notificación al alumno
        notificacionService.notificarReservaAlumnoCreada(
            alumnoId, ticket.getTicketId(), folio, espacio.getNombre());

        log.info("Reserva creada exitosamente: Ticket {}, Espacio {}", folio, espacio.getNombre());

        return ReservaAlumnoResponseDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(folio)
                .estado(ticket.getEstadoTicket().name())
                .mensaje("Reserva creada. Su comprobante está pendiente de revisión.")
                .build();
    }

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
}
