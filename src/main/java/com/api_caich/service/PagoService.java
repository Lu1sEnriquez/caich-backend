package com.api_caich.service;

import com.api_caich.dto.pago.PagoRequestDTO;
import com.api_caich.entity.Pago;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.PagoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Pago registrarPago(PagoRequestDTO pagoRequest, Long usuarioId) {
        log.info("Registrando pago para ticket: {}", pagoRequest.getTicketId());

        Ticket ticket = ticketRepository.findById(pagoRequest.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

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

        // Actualizar montos del ticket
        BigDecimal nuevoMontoPagado = ticket.getMontoPagado().add(pagoRequest.getMonto());
        ticket.setMontoPagado(nuevoMontoPagado);
        ticket.setEstadoFinanciero(EstadoFinanciero.EN_REVISION);
        ticketRepository.save(ticket);

        log.info("Pago {} registrado para ticket {}", pago.getPagoId(), ticket.getTicketId());
        return pago;
    }

    @Transactional
    public Pago aprobarPago(Long pagoId, Long usuarioId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (!EstadoPagoTransaccion.PENDIENTE_REVISION.equals(pago.getEstado())) {
            throw new BusinessException("Solo se pueden aprobar pagos en estado PENDIENTE_REVISION");
        }

        Usuario verificador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pago.setEstado(EstadoPagoTransaccion.APROBADO);
        pago.setVerificadoPor(verificador);
        pago = pagoRepository.save(pago);

        // Actualizar ticket
        Ticket ticket = pago.getTicket();
        if (ticket.getMontoPagado().compareTo(ticket.getMontoTotal()) >= 0) {
            ticket.setEstadoFinanciero(EstadoFinanciero.PAGADO);
        }
        ticketRepository.save(ticket);

        log.info("Pago {} aprobado", pagoId);
        return pago;
    }

    @Transactional
    public Pago rechazarPago(Long pagoId, String motivo, Long usuarioId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (!EstadoPagoTransaccion.PENDIENTE_REVISION.equals(pago.getEstado())) {
            throw new BusinessException("Solo se pueden rechazar pagos en estado PENDIENTE_REVISION");
        }

        Usuario verificador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        pago.setEstado(EstadoPagoTransaccion.RECHAZADO);
        pago.setVerificadoPor(verificador);
        pago.setMotivoRechazo(motivo);
        pago = pagoRepository.save(pago);

        // Revertir monto del ticket
        Ticket ticket = pago.getTicket();
        BigDecimal nuevoMontoPagado = ticket.getMontoPagado().subtract(pago.getMonto());
        ticket.setMontoPagado(nuevoMontoPagado);
        ticket.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
        ticketRepository.save(ticket);

        log.info("Pago {} rechazado", pagoId);
        return pago;
    }

    public List<Pago> obtenerPagosDeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        return pagoRepository.findAll().stream()
                .filter(p -> p.getTicket().equals(ticket))
                .collect(Collectors.toList());
    }

    public Page<Pago> obtenerTodosPagos(Pageable pageable) {
        List<Pago> todoPagos = pagoRepository.findAll();
        int start = Math.min((int) pageable.getOffset(), todoPagos.size());
        int end = Math.min((start + pageable.getPageSize()), todoPagos.size());
        List<Pago> pageContent = todoPagos.subList(start, end);
        return new PageImpl<>(pageContent, pageable, todoPagos.size());
    }

    public Page<Pago> obtenerPagosConEstado(EstadoPagoTransaccion estado, Pageable pageable) {
        List<Pago> pagos = pagoRepository.findAll().stream()
                .filter(p -> estado.equals(p.getEstado()))
                .collect(Collectors.toList());
        int start = Math.min((int) pageable.getOffset(), pagos.size());
        int end = Math.min((start + pageable.getPageSize()), pagos.size());
        List<Pago> pageContent = pagos.subList(start, end);
        return new PageImpl<>(pageContent, pageable, pagos.size());
    }

    public Pago obtenerPago(Long pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    }
}
