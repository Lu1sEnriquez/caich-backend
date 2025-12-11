package com.api_caich.service;

import com.api_caich.dto.pago.RegistrarPagoDTO;
import com.api_caich.dto.ticket.TicketDTO;
import com.api_caich.entity.Ticket;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final TicketRepository ticketRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public TicketDTO registrarPagoPropio(RegistrarPagoDTO pagoDTO, Long pacienteId) {
        // Verificar que el ticket existe y pertenece al paciente
        Ticket ticket = ticketRepository.findById(pagoDTO.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (!ticket.getPaciente().getUsuarioId().equals(pacienteId)) {
            throw new BusinessException("No tiene permisos para registrar pago en este ticket");
        }

        // Validar que el ticket esté en estado pendiente
        if (!"Pendiente".equals(ticket.getEstadoPago())) {
            throw new BusinessException("Solo se pueden registrar pagos en tickets con estado Pendiente");
        }

        // Validar que el monto sea correcto
        if (pagoDTO.getMontoPagado().compareTo(ticket.getCostoTotal()) < 0) {
            throw new BusinessException("El monto pagado no puede ser menor al costo total del ticket");
        }

        // Actualizar información de pago
        ticket.setMontoPagado(pagoDTO.getMontoPagado());
        ticket.setEstadoPago("Pagado");
        ticket.setConceptoTransferencia(pagoDTO.getConceptoTransferencia());
        ticket.setCelular(pagoDTO.getCelular());

        Ticket ticketActualizado = ticketRepository.save(ticket);

        log.info("Pago registrado por paciente {} para ticket {}", pacienteId, ticket.getTicketId());

        // Auditoría
        auditoriaService.registrarCambio("Tickets", ticket.getTicketId(), "Actualizar",
                "Pago registrado por paciente. EstadoPago: Pendiente -> Pagado",
                null, null, pacienteId, "SYSTEM");

        return mapToDTO(ticketActualizado);
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
