package com.api_caich.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.dto.pago.PagoRequestDTO;
import com.api_caich.entity.Pago;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.MetodoPago;
import com.api_caich.enums.Role;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.PagoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Pago registrarPago(PagoRequestDTO pagoRequest, Long usuarioId) {
        log.info("Registrando pago para ticket: {}", pagoRequest.getTicketId());

        Long ticketId = Objects.requireNonNull(pagoRequest.getTicketId(), "ticketId requerido");
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        validarPagoDirectoPaciente(ticket, usuario);

        // Validar monto
        if (pagoRequest.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a 0");
        }

        // Crear pago
        Pago pago = new Pago();
        pago.setTicket(ticket);
        pago.setMonto(pagoRequest.getMonto());
        pago.setMetodoPago(pagoRequest.getMetodoPago());
        pago.setComprobanteUrl(pagoRequest.getComprobanteUrl());
        pago.setReferencia(pagoRequest.getReferencia());
        pago.setEstado(EstadoPagoTransaccion.PENDIENTE_REVISION);
        pago.setFechaPago(LocalDateTime.now());

        pago = pagoRepository.save(pago);

        // Actualizar estado del ticket a EN_REVISION (sin contar el monto aún)
        // El monto solo se cuenta cuando el pago es APROBADO
        ticket.setEstadoFinanciero(EstadoFinanciero.EN_REVISION);
        ticketRepository.save(ticket);

        log.info("Pago {} registrado para ticket {}", pago.getPagoId(), ticket.getTicketId());
        return pago;
    }

    @Transactional
    public Pago aprobarPago(Long pagoId, Long usuarioId) {
        Pago pago = pagoRepository.findById(Objects.requireNonNull(pagoId, "pagoId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (!EstadoPagoTransaccion.PENDIENTE_REVISION.equals(pago.getEstado())) {
            throw new BusinessException("Solo se pueden aprobar pagos en estado PENDIENTE_REVISION");
        }

        Usuario verificador = usuarioRepository.findById(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pago.setEstado(EstadoPagoTransaccion.APROBADO);
        pago.setVerificadoPor(verificador);
        pago = pagoRepository.save(pago);

        // Actualizar ticket - Ahora sí contar el monto del pago aprobado
        Ticket ticket = pago.getTicket();
        BigDecimal nuevoMontoPagado = ticket.getMontoPagado().add(pago.getMonto());
        ticket.setMontoPagado(nuevoMontoPagado);
        
        // Recalcular estado financiero basado en el nuevo monto pagado
        if (nuevoMontoPagado.compareTo(ticket.getMontoTotal()) >= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        } else if (nuevoMontoPagado.compareTo(BigDecimal.ZERO) > 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
        } else {
            ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        }
        
        ticketRepository.save(ticket);

        log.info("Pago {} aprobado", pagoId);
        return pago;
    }

    @Transactional
    public Pago rechazarPago(Long pagoId, String motivo, Long usuarioId) {
        Pago pago = pagoRepository.findById(Objects.requireNonNull(pagoId, "pagoId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (!EstadoPagoTransaccion.PENDIENTE_REVISION.equals(pago.getEstado())) {
            throw new BusinessException("Solo se pueden rechazar pagos en estado PENDIENTE_REVISION");
        }

        Usuario verificador = usuarioRepository.findById(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pago.setEstado(EstadoPagoTransaccion.RECHAZADO);
        pago.setVerificadoPor(verificador);
        pago.setMotivoRechazo(motivo);
        pago = pagoRepository.save(pago);

        // IMPORTANTE: Los pagos rechazados NO afectan el montoPagado del ticket
        // porque nunca fueron sumados (solo se suman al ser APROBADOS).
        // El pago simplemente cambia de estado y queda registrado en el historial.

        log.info("Pago {} rechazado. montoPagado del ticket NO se modifica.", pagoId);
        return pago;
    }

    @Transactional
    public Pago cancelarPago(Long pagoId, String motivo, Long usuarioId) {
        Pago pago = pagoRepository.findById(Objects.requireNonNull(pagoId, "pagoId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (!EstadoPagoTransaccion.APROBADO.equals(pago.getEstado())) {
            throw new BusinessException("Solo se pueden cancelar pagos en estado APROBADO");
        }
        if (MetodoPago.MONEDERO.equals(pago.getMetodoPago())) {
            throw new BusinessException(
                    "Los pagos de monedero no se cancelan directamente. Cancele el ticket para iniciar la devolución.");
        }

        Usuario cancelador = usuarioRepository.findById(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pago.setEstado(EstadoPagoTransaccion.CANCELADO);
        pago.setVerificadoPor(cancelador);
        pago.setMotivoRechazo(motivo); // Reutilizar el campo para guardar el motivo de cancelación
        pago = pagoRepository.save(pago);

        // Revertir monto del ticket
        Ticket ticket = pago.getTicket();
        BigDecimal nuevoMontoPagado = ticket.getMontoPagado().subtract(pago.getMonto());
        ticket.setMontoPagado(nuevoMontoPagado);

        // Recalcular estado financiero
        if (nuevoMontoPagado.compareTo(BigDecimal.ZERO) <= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        } else if (nuevoMontoPagado.compareTo(ticket.getMontoTotal()) >= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        } else {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
        }

        ticketRepository.save(ticket);

        log.info("Pago {} cancelado", pagoId);
        return pago;
    }

    public List<Pago> obtenerPagosDeTicket(Long ticketId) {
        ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        return pagoRepository.findByTicket_TicketId(ticketId);
    }

    public Page<Pago> obtenerTodosPagos(Pageable pageable) {
        return pagoRepository.findByMetodoPagoNot(MetodoPago.MONEDERO, pageable);
    }

    public Page<Pago> obtenerPagosConEstado(EstadoPagoTransaccion estado, Pageable pageable) {
        return pagoRepository.findByEstadoAndMetodoPagoNot(
                estado, MetodoPago.MONEDERO, pageable);
    }

    public Pago obtenerPago(Long pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    }

    /**
     * Registra un pago subido por paciente con comprobante.
     * Estado: PENDIENTE_REVISION — requiere aprobación del admin.
     */
    @Transactional
    public Pago registrarPagoConComprobante(Long ticketId, BigDecimal monto, String metodoPago,
                                             String referencia, String notas, String comprobanteUrl, Long usuarioId) {
        log.info("Registrando pago con comprobante para ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        validarPagoDirectoPaciente(ticket, usuario);

        // Validar monto
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a 0");
        }

        // Validar que el monto no exceda el saldo pendiente
        BigDecimal saldoPendiente = ticket.getMontoTotal().subtract(ticket.getMontoPagado());
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new BusinessException(
                    String.format("El monto ($%.2f) excede el saldo pendiente ($%.2f)", monto, saldoPendiente));
        }

        Pago pago = new Pago();
        pago.setTicket(ticket);
        pago.setMonto(monto);
        pago.setMetodoPago(com.api_caich.enums.MetodoPago.valueOf(metodoPago));
        pago.setReferencia(referencia);
        pago.setComprobanteUrl(comprobanteUrl);
        pago.setNotas(notas);
        pago.setEstado(EstadoPagoTransaccion.PENDIENTE_REVISION);
        pago.setFechaPago(LocalDateTime.now());

        pago = pagoRepository.save(pago);

        // Actualizar estado financiero del ticket
        ticket.setEstadoFinanciero(EstadoFinanciero.EN_REVISION);
        ticketRepository.save(ticket);

        log.info("Pago {} con comprobante registrado para ticket {}", pago.getPagoId(), ticketId);
        return pago;
    }

    /**
     * Registra un pago manual por un admin. Se aprueba automáticamente
     * y se recalcula el estado financiero del ticket.
     */
    @Transactional
    public Pago registrarPagoManual(Long ticketId, BigDecimal monto, String metodoPago,
                                     String referencia, String notas, String comprobanteUrl, Long adminId) {
        log.info("Registrando pago manual para ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(Objects.requireNonNull(ticketId, "ticketId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Usuario admin = usuarioRepository.findById(Objects.requireNonNull(adminId, "adminId requerido"))
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado"));
        validarPagoDirectoPaciente(ticket, admin);

        // Validar monto
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a 0");
        }

        // Validar que el monto no exceda el saldo pendiente
        BigDecimal saldoPendiente = ticket.getMontoTotal().subtract(ticket.getMontoPagado());
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new BusinessException(
                    String.format("El monto ($%.2f) excede el saldo pendiente ($%.2f)", monto, saldoPendiente));
        }

        Pago pago = new Pago();
        pago.setTicket(ticket);
        pago.setMonto(monto);
        pago.setMetodoPago(com.api_caich.enums.MetodoPago.valueOf(metodoPago));
        pago.setReferencia(referencia);
        pago.setComprobanteUrl(comprobanteUrl);
        pago.setNotas(notas);
        pago.setEstado(EstadoPagoTransaccion.APROBADO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setVerificadoPor(admin);

        pago = pagoRepository.save(pago);

        // Actualizar montos del ticket
        BigDecimal nuevoMontoPagado = ticket.getMontoPagado().add(monto);
        ticket.setMontoPagado(nuevoMontoPagado);

        // Recalcular estado financiero
        if (nuevoMontoPagado.compareTo(ticket.getMontoTotal()) >= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        } else if (nuevoMontoPagado.compareTo(BigDecimal.ZERO) > 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
        } else {
            ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        }

        ticketRepository.save(ticket);

        log.info("Pago manual {} registrado y aprobado para ticket {}", pago.getPagoId(), ticketId);
        return pago;
    }

    private void validarPagoDirectoPaciente(Ticket ticket, Usuario usuario) {
        if (ticket.getCita() == null || ticket.getCita().getPaciente() == null) {
            throw new BusinessException(
                    "Los pagos directos solo están disponibles para tickets de pacientes. Los alumnos deben usar monedero.");
        }

        boolean esAdministrador = usuario.getRol().equalsIgnoreCase(Role.ADMINISTRADOR.getNombre());
        boolean esPacienteDelTicket = ticket.getCita().getPaciente().getUsuarioId()
                .equals(usuario.getUsuarioId());

        if (!esAdministrador && !esPacienteDelTicket) {
            throw new BusinessException("No puede registrar pagos para el ticket de otro paciente");
        }
    }
}
