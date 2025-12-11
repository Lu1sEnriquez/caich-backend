package com.api_caich.service;

import com.api_caich.entity.Ticket;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private final TicketRepository ticketRepository;
    private final FileStorageService fileStorageService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public void subirComprobantePago(Long ticketId, MultipartFile archivo, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        // Validar permisos - paciente solo puede subir comprobante de sus tickets
        if (!ticket.getPaciente().getUsuarioId().equals(usuarioId)) {
            throw new BusinessException("No tiene permisos para subir comprobante para este ticket");
        }

        // Validar que el ticket esté pagado
        if (!"Pagado".equals(ticket.getEstadoPago())) {
            throw new BusinessException("Solo se pueden subir comprobantes para tickets con estado Pagado");
        }

        try {
            // Almacenar comprobante
            String comprobanteUrl = fileStorageService.almacenarComprobante(archivo, ticketId);

            // Actualizar ticket con referencia al comprobante
            // Podrías agregar un campo en Ticket para almacenar la URL del comprobante
            // Por ahora, asumiremos que se almacena en el campo de notas
            String notasActualizadas = ticket.getNotas() != null
                    ? ticket.getNotas() + "\n[Comprobante subido: " + comprobanteUrl + "]"
                    : "Comprobante subido: " + comprobanteUrl;

            ticket.setNotas(notasActualizadas);
            ticketRepository.save(ticket);

            log.info("Comprobante subido para ticket {} por usuario {}", ticketId, usuarioId);

            // Auditoría
            auditoriaService.registrarCambio("Tickets", ticketId, "Actualizar",
                    "Comprobante de pago subido", null, null, usuarioId, "SYSTEM");

        } catch (Exception e) {
            log.error("Error al subir comprobante para ticket {}", ticketId, e);
            throw new BusinessException("Error al subir comprobante: " + e.getMessage());
        }
    }
}