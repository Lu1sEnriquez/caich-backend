package com.api_caich.dto.ticket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketDTO {
    private Long ticketId;
    private String folio;
    private LocalDateTime fecha;
    private String pacienteNombre;
    private String terapeutaNombre;
    private String espacioNombre;
    private BigDecimal costoTotal;
    private String estadoTicket;
    private String estadoPago;
    private List<TicketDetalleDTO> detalles;
    
    // Método para mapear desde la entidad
    public static TicketDTO fromEntity(com.api_caich.entity.Ticket ticket) {
        List<TicketDetalleDTO> detallesDTO = ticket.getDetalles().stream()
                .map(detalle -> TicketDetalleDTO.builder()
                        .detalleId(detalle.getDetalleId())
                        .productoId(detalle.getProducto().getProductoId())
                        .productoNombre(detalle.getProducto().getNombre())
                        .productoCodigo(detalle.getProducto().getCodigo())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getPrecioUnitario())
                        .subtotal(detalle.getSubtotal())
                        .tipoUso(detalle.getTipoUso())
                        .build())
                .toList();
        
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
                .detalles(detallesDTO)
                .build();
    }
}