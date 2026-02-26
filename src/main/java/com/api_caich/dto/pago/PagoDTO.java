package com.api_caich.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api_caich.entity.Pago;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.MetodoPago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDTO {

    private Long pagoId;
    private Long ticketId;
    private String ticketFolio;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String comprobanteUrl;
    private String referencia;
    private EstadoPagoTransaccion estado;
    private String motivoRechazo;
    private String notas;
    private String verificadoPorNombre;
    private LocalDateTime fechaPago;
    private LocalDateTime fechaCreacion;

    public static PagoDTO fromEntity(Pago pago) {
        if (pago == null) {
            return null;
        }

        return PagoDTO.builder()
                .pagoId(pago.getPagoId())
                .ticketId(pago.getTicket() != null ? pago.getTicket().getTicketId() : null)
                .ticketFolio(pago.getTicket() != null ? pago.getTicket().getFolio() : null)
                .monto(pago.getMonto())
                .metodoPago(pago.getMetodoPago())
                .comprobanteUrl(pago.getComprobanteUrl())
                .referencia(pago.getReferencia())
                .estado(pago.getEstado())
                .motivoRechazo(pago.getMotivoRechazo())
                .notas(pago.getNotas())
                .verificadoPorNombre(pago.getVerificadoPor() != null ? pago.getVerificadoPor().getNombreCompleto() : null)
                .fechaPago(pago.getFechaPago())
                .fechaCreacion(pago.getFechaCreacion())
                .build();
    }
}
