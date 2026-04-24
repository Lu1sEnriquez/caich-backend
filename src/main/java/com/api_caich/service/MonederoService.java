package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.dto.monedero.CrearRecargaMonederoRequest;
import com.api_caich.dto.monedero.MonederoDTO;
import com.api_caich.dto.monedero.MovimientoMonederoDTO;
import com.api_caich.dto.monedero.RecargaMonederoDTO;
import com.api_caich.dto.monedero.RechazarRequest;
import com.api_caich.dto.monedero.SolicitudDevolucionDTO;
import com.api_caich.entity.Monedero;
import com.api_caich.entity.MovimientoMonedero;
import com.api_caich.entity.RecargaMonedero;
import com.api_caich.entity.SolicitudDevolucionMonedero;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoDevolucionMonedero;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.EstadoRecargaMonedero;
import com.api_caich.enums.MetodoPago;
import com.api_caich.enums.Role;
import com.api_caich.enums.TipoMovimientoMonedero;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.MonederoRepository;
import com.api_caich.repository.MovimientoMonederoRepository;
import com.api_caich.repository.PagoRepository;
import com.api_caich.repository.RecargaMonederoRepository;
import com.api_caich.repository.SolicitudDevolucionMonederoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonederoService {

    private final MonederoRepository monederoRepository;
    private final MovimientoMonederoRepository movimientoRepository;
    private final RecargaMonederoRepository recargaRepository;
    private final SolicitudDevolucionMonederoRepository devolucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final PagoRepository pagoRepository;
    private final FileStorageService fileStorageService;
    private final NotificacionService notificacionService;

    // -------------------------------------------------------------------------
    // CONSULTAS DEL ALUMNO
    // -------------------------------------------------------------------------

    /**
     * Obtiene (o crea) el monedero de un alumno.
     * Cualquier alumno autenticado puede ver su propio monedero.
     */
    @Transactional
    public MonederoDTO obtenerMonedero(Long alumnoId) {
        Monedero monedero = obtenerOCrearMonedero(alumnoId);
        return toMonederoDTO(monedero);
    }

    /**
     * Movimientos paginados del alumno autenticado.
     */
    @Transactional(readOnly = true)
    public Page<MovimientoMonederoDTO> obtenerMisMovimientos(Long alumnoId, Pageable pageable) {
        Monedero monedero = monederoRepository.findByAlumno_UsuarioId(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Monedero no encontrado"));
        return movimientoRepository
                .findByMonedero_MonederoId(monedero.getMonederoId(), pageable)
                .map(m -> toMovimientoDTO(m, false));
    }

    // -------------------------------------------------------------------------
    // RECARGAS — ALUMNO
    // -------------------------------------------------------------------------

    /**
     * El alumno solicita una recarga: sube su comprobante y queda PENDIENTE_REVISION.
     */
    @Transactional
    public RecargaMonederoDTO solicitarRecarga(Long alumnoId,
                                               CrearRecargaMonederoRequest request,
                                               MultipartFile comprobante) {
        Usuario alumno = validarAlumno(alumnoId);

        // Guardar comprobante en disco / Drive
        String comprobanteUrl = fileStorageService.almacenarComprobanteRecarga(comprobante, alumnoId);

        RecargaMonedero recarga = new RecargaMonedero();
        recarga.setAlumno(alumno);
        recarga.setMonto(request.getMonto());
        recarga.setComprobanteUrl(comprobanteUrl);
        recarga.setReferenciaBancaria(request.getReferenciaBancaria());
        recarga.setNotasAlumno(request.getNotasAlumno());
        recarga.setEstado(EstadoRecargaMonedero.PENDIENTE_REVISION);
        recarga.setNumeroIntento(1);

        recarga = recargaRepository.save(recarga);

        // Notificar al admin
        notificacionService.notificarNuevaRecargaMonedero(alumnoId, recarga.getRecargaMonederoId(), request.getMonto());

        return toRecargaDTO(recarga);
    }

    /**
     * El alumno reenvía un comprobante corregido tras un rechazo.
     */
    @Transactional
    public RecargaMonederoDTO reintentarRecarga(Long alumnoId,
                                                Long recargaId,
                                                CrearRecargaMonederoRequest request,
                                                MultipartFile comprobante) {
        RecargaMonedero recarga = recargaRepository.findById(recargaId)
                .orElseThrow(() -> new ResourceNotFoundException("Recarga no encontrada"));

        if (!recarga.getAlumno().getUsuarioId().equals(alumnoId)) {
            throw new BusinessException("No tiene permiso para modificar esta recarga");
        }
        if (recarga.getEstado() != EstadoRecargaMonedero.RECHAZADO) {
            throw new BusinessException("Solo se puede reintentar una recarga rechazada");
        }

        String comprobanteUrl = fileStorageService.almacenarComprobanteRecarga(comprobante, alumnoId);

        recarga.setMonto(request.getMonto());
        recarga.setComprobanteUrl(comprobanteUrl);
        recarga.setReferenciaBancaria(request.getReferenciaBancaria());
        recarga.setNotasAlumno(request.getNotasAlumno());
        recarga.setEstado(EstadoRecargaMonedero.PENDIENTE_REVISION);
        recarga.setMotivoRechazo(null);
        recarga.setVerificadoPor(null);
        recarga.setFechaVerificacion(null);
        recarga.setNumeroIntento(recarga.getNumeroIntento() + 1);

        recarga = recargaRepository.save(recarga);
        notificacionService.notificarNuevaRecargaMonedero(alumnoId, recarga.getRecargaMonederoId(), request.getMonto());
        return toRecargaDTO(recarga);
    }

    /**
     * Historial de recargas del alumno autenticado.
     */
    @Transactional(readOnly = true)
    public List<RecargaMonederoDTO> obtenerMisRecargas(Long alumnoId) {
        return recargaRepository.findByAlumno_UsuarioIdOrderByFechaSolicitudDesc(alumnoId)
                .stream().map(this::toRecargaDTO).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // RECARGAS — ADMIN
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RecargaMonederoDTO> obtenerRecargasPendientes() {
        return recargaRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoRecargaMonedero.PENDIENTE_REVISION)
                .stream().map(this::toRecargaDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RecargaMonederoDTO> obtenerTodasLasRecargas(Pageable pageable) {
        return recargaRepository.findAllByOrderByFechaSolicitudDesc(pageable).map(this::toRecargaDTO);
    }

    /**
     * Admin aprueba una recarga: acredita el saldo al monedero con locking optimista.
     */
    @Transactional
    public RecargaMonederoDTO aprobarRecarga(Long recargaId, Long adminId) {
        RecargaMonedero recarga = recargaRepository.findById(recargaId)
                .orElseThrow(() -> new ResourceNotFoundException("Recarga no encontrada"));

        if (recarga.getEstado() != EstadoRecargaMonedero.PENDIENTE_REVISION) {
            throw new BusinessException("La recarga ya fue procesada (estado: " + recarga.getEstado() + ")");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        // Acreditar saldo (con manejo de conflicto optimista)
        try {
            Monedero monedero = obtenerOCrearMonedero(recarga.getAlumno().getUsuarioId());
            BigDecimal saldoAnterior = monedero.getSaldo();
            BigDecimal saldoNuevo = saldoAnterior.add(recarga.getMonto());

            monedero.setSaldo(saldoNuevo);
            monederoRepository.save(monedero);

            // Registrar movimiento
            registrarMovimiento(monedero, TipoMovimientoMonedero.RECARGA,
                    recarga.getMonto(), saldoAnterior, saldoNuevo,
                    "Recarga aprobada por " + admin.getNombreCompleto(),
                    null, recarga);

            // Actualizar estado de la recarga
            recarga.setEstado(EstadoRecargaMonedero.APROBADO);
            recarga.setVerificadoPor(admin);
            recarga.setFechaVerificacion(LocalDateTime.now());
            recarga = recargaRepository.save(recarga);

        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("El monedero fue modificado concurrentemente. Intente de nuevo.");
        }

        notificacionService.notificarRecargaAprobada(
                recarga.getAlumno().getUsuarioId(), recarga.getMonto());

        log.info("Recarga {} aprobada por admin {}. Monto: {}", recargaId, adminId, recarga.getMonto());
        return toRecargaDTO(recarga);
    }

    /**
     * Admin rechaza una recarga: no se acredita nada; alumno puede reintentar.
     */
    @Transactional
    public RecargaMonederoDTO rechazarRecarga(Long recargaId, Long adminId, RechazarRequest req) {
        RecargaMonedero recarga = recargaRepository.findById(recargaId)
                .orElseThrow(() -> new ResourceNotFoundException("Recarga no encontrada"));

        if (recarga.getEstado() != EstadoRecargaMonedero.PENDIENTE_REVISION) {
            throw new BusinessException("La recarga ya fue procesada (estado: " + recarga.getEstado() + ")");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        recarga.setEstado(EstadoRecargaMonedero.RECHAZADO);
        recarga.setMotivoRechazo(req.getMotivo());
        recarga.setVerificadoPor(admin);
        recarga.setFechaVerificacion(LocalDateTime.now());
        recarga = recargaRepository.save(recarga);

        notificacionService.notificarRecargaRechazada(
                recarga.getAlumno().getUsuarioId(), recarga.getMonto(), req.getMotivo());

        log.info("Recarga {} rechazada por admin {}. Motivo: {}", recargaId, adminId, req.getMotivo());
        return toRecargaDTO(recarga);
    }

    // -------------------------------------------------------------------------
    // PAGO CON MONEDERO (llamado desde ReservaAlumnoService)
    // -------------------------------------------------------------------------

    /**
     * Descuenta el monto del monedero del alumno de forma atómica.
     * Lanza BusinessException si el saldo es insuficiente.
     * Retorna el Pago creado para asociarlo al ticket.
     */
    @Transactional
    public com.api_caich.entity.Pago descontarSaldo(Long alumnoId,
                                                     Ticket ticket,
                                                     BigDecimal monto) {
        try {
            Monedero monedero = monederoRepository.findByAlumno_UsuarioId(alumnoId)
                    .orElseThrow(() -> new BusinessException("El alumno no tiene monedero activo"));

            if (monedero.getSaldo().compareTo(monto) < 0) {
                throw new BusinessException(
                        "Saldo insuficiente. Disponible: $" + monedero.getSaldo() +
                        " | Requerido: $" + monto);
            }

            BigDecimal saldoAnterior = monedero.getSaldo();
            BigDecimal saldoNuevo = saldoAnterior.subtract(monto);
            monedero.setSaldo(saldoNuevo);
            monederoRepository.save(monedero);

            registrarMovimiento(monedero, TipoMovimientoMonedero.CONSUMO,
                    monto, saldoAnterior, saldoNuevo,
                    "Pago de reserva: " + ticket.getFolio(),
                    ticket, null);

            // Crear registro Pago con estado APROBADO (no necesita validación manual)
            com.api_caich.entity.Pago pago = new com.api_caich.entity.Pago();
            pago.setTicket(ticket);
            pago.setMonto(monto);
            pago.setMetodoPago(MetodoPago.MONEDERO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setNotas("Descuento automático del monedero virtual");
            pago.setFechaPago(LocalDateTime.now());
            return pagoRepository.save(pago);

        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("El monedero fue modificado concurrentemente. Intente de nuevo.");
        }
    }

    // -------------------------------------------------------------------------
    // DEVOLUCIONES — generadas al cancelar ticket pagado con monedero
    // -------------------------------------------------------------------------

    /**
     * Crea una solicitud de devolución pendiente cuando se cancela un ticket
     * que fue pagado con monedero. El admin debe aprobarla manualmente.
     */
    @Transactional
    public void crearSolicitudDevolucion(Ticket ticket) {
        // Evitar duplicados
        if (devolucionRepository.existsByTicket_TicketId(ticket.getTicketId())) {
            log.warn("Ya existe solicitud de devolución para ticket {}", ticket.getTicketId());
            return;
        }

        // Calcular monto a devolver (lo que se pagó con monedero)
        BigDecimal montoMonedero = ticket.getPagos().stream()
                .filter(p -> p.getMetodoPago() == MetodoPago.MONEDERO
                          && p.getEstado() == EstadoPagoTransaccion.APROBADO)
                .map(com.api_caich.entity.Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (montoMonedero.compareTo(BigDecimal.ZERO) <= 0) {
            return; // No hay nada que devolver
        }

        Monedero monedero = monederoRepository.findByAlumno_UsuarioId(ticket.getCreadoPor().getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Monedero no encontrado para devolución"));

        SolicitudDevolucionMonedero solicitud = new SolicitudDevolucionMonedero();
        solicitud.setTicket(ticket);
        solicitud.setMonedero(monedero);
        solicitud.setMontoDevolucion(montoMonedero);
        solicitud.setEstado(EstadoDevolucionMonedero.PENDIENTE);
        devolucionRepository.save(solicitud);

        // Notificar admin
        notificacionService.notificarSolicitudDevolucion(
                ticket.getCreadoPor().getUsuarioId(),
                ticket.getTicketId(), montoMonedero);

        log.info("Solicitud de devolución creada para ticket {}. Monto: {}", ticket.getFolio(), montoMonedero);
    }

    // -------------------------------------------------------------------------
    // DEVOLUCIONES — ADMIN
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SolicitudDevolucionDTO> obtenerDevolucionesPendientes() {
        return devolucionRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoDevolucionMonedero.PENDIENTE)
                .stream().map(this::toDevolucionDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudDevolucionDTO> obtenerTodasLasDevoluciones() {
        return devolucionRepository.findAll().stream()
                .map(this::toDevolucionDTO).collect(Collectors.toList());
    }

    /**
     * Admin aprueba la devolución: acredita el monto al monedero.
     */
    @Transactional
    public SolicitudDevolucionDTO aprobarDevolucion(Long solicitudId, Long adminId) {
        SolicitudDevolucionMonedero solicitud = devolucionRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de devolución no encontrada"));

        if (solicitud.getEstado() != EstadoDevolucionMonedero.PENDIENTE) {
            throw new BusinessException("Esta solicitud ya fue procesada (estado: " + solicitud.getEstado() + ")");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        try {
            Monedero monedero = solicitud.getMonedero();
            BigDecimal saldoAnterior = monedero.getSaldo();
            BigDecimal saldoNuevo = saldoAnterior.add(solicitud.getMontoDevolucion());

            monedero.setSaldo(saldoNuevo);
            monederoRepository.save(monedero);

            registrarMovimiento(monedero, TipoMovimientoMonedero.DEVOLUCION_CANCELACION,
                    solicitud.getMontoDevolucion(), saldoAnterior, saldoNuevo,
                    "Devolución por cancelación de reserva: " + solicitud.getTicket().getFolio(),
                    solicitud.getTicket(), null);

            // Marcar pagos con monedero del ticket como CANCELADO
            solicitud.getTicket().getPagos().stream()
                    .filter(p -> p.getMetodoPago() == MetodoPago.MONEDERO)
                    .forEach(p -> {
                        p.setEstado(EstadoPagoTransaccion.CANCELADO);
                        pagoRepository.save(p);
                    });

            solicitud.setEstado(EstadoDevolucionMonedero.APROBADA);
            solicitud.setProcesadoPor(admin);
            solicitud.setFechaProcesamiento(LocalDateTime.now());
            solicitud = devolucionRepository.save(solicitud);

            // Actualizar estado financiero del ticket
            Ticket ticket = solicitud.getTicket();
            ticket.setEstadoFinanciero(EstadoFinanciero.REEMBOLSADO);
            ticketRepository.save(ticket);

        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("El monedero fue modificado concurrentemente. Intente de nuevo.");
        }

        notificacionService.notificarDevolucionAprobada(
                solicitud.getMonedero().getAlumno().getUsuarioId(),
                solicitud.getMontoDevolucion());

        return toDevolucionDTO(solicitud);
    }

    /**
     * Admin rechaza la devolución (ej. cancelación fuera de política).
     */
    @Transactional
    public SolicitudDevolucionDTO rechazarDevolucion(Long solicitudId, Long adminId, RechazarRequest req) {
        SolicitudDevolucionMonedero solicitud = devolucionRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de devolución no encontrada"));

        if (solicitud.getEstado() != EstadoDevolucionMonedero.PENDIENTE) {
            throw new BusinessException("Esta solicitud ya fue procesada (estado: " + solicitud.getEstado() + ")");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        solicitud.setEstado(EstadoDevolucionMonedero.RECHAZADA);
        solicitud.setMotivoRechazo(req.getMotivo());
        solicitud.setProcesadoPor(admin);
        solicitud.setFechaProcesamiento(LocalDateTime.now());
        solicitud = devolucionRepository.save(solicitud);

        return toDevolucionDTO(solicitud);
    }

    // -------------------------------------------------------------------------
    // VISTA ADMIN — MONEDEROS DE TODOS LOS ALUMNOS
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MonederoDTO> obtenerTodosLosMonederos() {
        return monederoRepository.findAll().stream()
                .map(this::toMonederoDTO).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // MOVIMIENTOS ADMIN (para reporte y Excel)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MovimientoMonederoDTO> obtenerMovimientosAdmin(
            Long alumnoId, LocalDate desde, LocalDate hasta) {

        LocalDateTime dtDesde = (desde != null) ? desde.atStartOfDay() : LocalDate.of(2020, 1, 1).atStartOfDay();
        LocalDateTime dtHasta = (hasta != null) ? hasta.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

        if (alumnoId != null) {
            return movimientoRepository.findByAlumnoAndRangoFechas(alumnoId, dtDesde, dtHasta)
                    .stream().map(m -> toMovimientoDTO(m, true)).collect(Collectors.toList());
        }
        return movimientoRepository.findByRangoFechas(dtDesde, dtHasta)
                .stream().map(m -> toMovimientoDTO(m, true)).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // CONTADORES PARA BADGES (llamados por DashboardController o similar)
    // -------------------------------------------------------------------------

    public long contarRecargasPendientes() {
        return recargaRepository.countByEstado(EstadoRecargaMonedero.PENDIENTE_REVISION);
    }

    public long contarDevolucionesPendientes() {
        return devolucionRepository.countByEstado(EstadoDevolucionMonedero.PENDIENTE);
    }

    // -------------------------------------------------------------------------
    // HELPERS INTERNOS
    // -------------------------------------------------------------------------

    /** Obtiene el monedero del alumno o lo crea si no existe. */
    @Transactional
    public Monedero obtenerOCrearMonedero(Long alumnoId) {
        return monederoRepository.findByAlumno_UsuarioId(alumnoId).orElseGet(() -> {
            Usuario alumno = usuarioRepository.findById(alumnoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + alumnoId));
            Monedero nuevo = new Monedero();
            nuevo.setAlumno(alumno);
            nuevo.setSaldo(BigDecimal.ZERO);
            return monederoRepository.save(nuevo);
        });
    }

    private void registrarMovimiento(Monedero monedero, TipoMovimientoMonedero tipo,
                                     BigDecimal monto, BigDecimal saldoAnterior,
                                     BigDecimal saldoNuevo, String descripcion,
                                     Ticket ticket, RecargaMonedero recarga) {
        MovimientoMonedero mov = new MovimientoMonedero();
        mov.setMonedero(monedero);
        mov.setTipo(tipo);
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setDescripcion(descripcion);
        mov.setTicket(ticket);
        mov.setRecargaMonedero(recarga);
        mov.setFechaMovimiento(LocalDateTime.now());
        movimientoRepository.save(mov);
    }

    private Usuario validarAlumno(Long alumnoId) {
        Usuario usuario = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        boolean tieneAcceso = usuario.getRol().equalsIgnoreCase(Role.ALUMNO.getNombre())
                || usuario.getRol().equalsIgnoreCase(Role.TERAPEUTA.getNombre());
        if (!tieneAcceso) {
            throw new BusinessException("Solo los alumnos y terapeutas pueden usar el monedero");
        }
        return usuario;
    }

    // -------------------------------------------------------------------------
    // MAPPERS
    // -------------------------------------------------------------------------

    private MonederoDTO toMonederoDTO(Monedero m) {
        return MonederoDTO.builder()
                .monederoId(m.getMonederoId())
                .alumnoId(m.getAlumno().getUsuarioId())
                .alumnoNombre(m.getAlumno().getNombreCompleto())
                .alumnoEmail(m.getAlumno().getEmail())
                .alumnoFolio(m.getAlumno().getFolio())
                .saldo(m.getSaldo())
                .fechaCreacion(m.getFechaCreacion())
                .ultimaActualizacion(m.getUltimaActualizacion())
                .build();
    }

    private MovimientoMonederoDTO toMovimientoDTO(MovimientoMonedero m, boolean incluirAlumno) {
        Long ticketId = m.getTicket() != null ? m.getTicket().getTicketId() : null;
        String ticketFolio = m.getTicket() != null ? m.getTicket().getFolio() : null;
        Long recargaId = m.getRecargaMonedero() != null ? m.getRecargaMonedero().getRecargaMonederoId() : null;
        Long aId = incluirAlumno ? m.getMonedero().getAlumno().getUsuarioId() : null;
        String aNombre = incluirAlumno ? m.getMonedero().getAlumno().getNombreCompleto() : null;

        return MovimientoMonederoDTO.builder()
                .movimientoId(m.getMovimientoId())
                .tipo(m.getTipo().name())
                .monto(m.getMonto())
                .saldoAnterior(m.getSaldoAnterior())
                .saldoNuevo(m.getSaldoNuevo())
                .descripcion(m.getDescripcion())
                .fechaMovimiento(m.getFechaMovimiento())
                .ticketId(ticketId)
                .ticketFolio(ticketFolio)
                .recargaMonederoId(recargaId)
                .alumnoId(aId)
                .alumnoNombre(aNombre)
                .build();
    }

    private RecargaMonederoDTO toRecargaDTO(RecargaMonedero r) {
        return RecargaMonederoDTO.builder()
                .recargaMonederoId(r.getRecargaMonederoId())
                .alumnoId(r.getAlumno().getUsuarioId())
                .alumnoNombre(r.getAlumno().getNombreCompleto())
                .alumnoEmail(r.getAlumno().getEmail())
                .alumnoFolio(r.getAlumno().getFolio())
                .monto(r.getMonto())
                .comprobanteUrl(r.getComprobanteUrl())
                .referenciaBancaria(r.getReferenciaBancaria())
                .notasAlumno(r.getNotasAlumno())
                .estado(r.getEstado().name())
                .motivoRechazo(r.getMotivoRechazo())
                .verificadoPorNombre(r.getVerificadoPor() != null ? r.getVerificadoPor().getNombreCompleto() : null)
                .fechaSolicitud(r.getFechaSolicitud())
                .fechaVerificacion(r.getFechaVerificacion())
                .numeroIntento(r.getNumeroIntento())
                .build();
    }

    private SolicitudDevolucionDTO toDevolucionDTO(SolicitudDevolucionMonedero s) {
        return SolicitudDevolucionDTO.builder()
                .solicitudDevolucionId(s.getSolicitudDevolucionId())
                .ticketId(s.getTicket().getTicketId())
                .ticketFolio(s.getTicket().getFolio())
                .alumnoId(s.getMonedero().getAlumno().getUsuarioId())
                .alumnoNombre(s.getMonedero().getAlumno().getNombreCompleto())
                .montoDevolucion(s.getMontoDevolucion())
                .estado(s.getEstado().name())
                .motivoRechazo(s.getMotivoRechazo())
                .procesadoPorNombre(s.getProcesadoPor() != null ? s.getProcesadoPor().getNombreCompleto() : null)
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaProcesamiento(s.getFechaProcesamiento())
                .build();
    }
}
