package com.api_caich.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketDTO {

    private Long ticketId;
    private String folio;

    // Usuario creador
    private Long creadoPorId;
    private String creadoPorNombre;

    // Estados (enums)
    private EstadoTicket estadoTicket;
    private EstadoFinanciero estadoFinanciero;

    // Componentes modulares
    @Builder.Default
    private List<TicketVentaDTO> ventas = new ArrayList<>();

    private TicketCitaDTO cita;

    @Builder.Default
    private List<TicketEspacioDTO> espacios = new ArrayList<>();

    @Builder.Default
    private List<TicketPrestamoDTO> prestamos = new ArrayList<>();

    // Costo adicional
    private BigDecimal costoAdicional;
    private String motivoCostoAdicional;

    // Totales financieros
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;

    // Cantidad de pagos registrados
    private Integer cantidadPagos;

    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;

    /**
     * Mapea una entidad Ticket a TicketDTO con arquitectura modular
     */
    public static TicketDTO fromEntity(com.api_caich.entity.Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        // Mapear ventas
        List<TicketVentaDTO> ventasDTO = new ArrayList<>();
        if (ticket.getVentas() != null) {
            ventasDTO = ticket.getVentas().stream()
                    .map(venta -> TicketVentaDTO.builder()
                    .ventaId(venta.getId())
                    .productoId(venta.getProducto().getProductoId())
                    .productoNombre(venta.getProducto().getNombre())
                    .productoCodigo(venta.getProducto().getCodigo())
                    .cantidad(venta.getCantidad())
                    .precioUnitario(venta.getPrecioUnitario())
                    .subtotal(venta.getSubtotal())
                    .build())
                    .collect(Collectors.toList());
        }

        // Mapear cita (opcional)
        TicketCitaDTO citaDTO = null;
        if (ticket.getCita() != null) {
            com.api_caich.entity.TicketCita cita = ticket.getCita();
            citaDTO = TicketCitaDTO.builder()
                    .citaId(cita.getId())
                    .pacienteId(cita.getPaciente() != null ? cita.getPaciente().getUsuarioId() : null)
                    .pacienteNombre(cita.getPaciente() != null ? cita.getPaciente().getNombreCompleto() : null)
                    .terapeutaId(cita.getTerapeuta() != null ? cita.getTerapeuta().getUsuarioId() : null)
                    .terapeutaNombre(cita.getTerapeuta() != null ? cita.getTerapeuta().getNombreCompleto() : null)
                    .fechaInicio(cita.getFechaInicio())
                    .fechaFin(cita.getFechaFin())
                    .precioCobrado(cita.getPrecioCobrado())
                    .notasTerapia(cita.getNotasTerapia())
                    .modalidad(cita.getModalidad())
                    .build();
        }

        // Mapear espacios
        List<TicketEspacioDTO> espaciosDTO = new ArrayList<>();
        if (ticket.getEspacios() != null) {
            espaciosDTO = ticket.getEspacios().stream()
                    .map(espacio -> TicketEspacioDTO.builder()
                    .espacioReservaId(espacio.getId())
                    .espacioId(espacio.getEspacio().getEspacioId())
                    .espacioNombre(espacio.getEspacio().getNombre())
                    .horaInicioReserva(espacio.getHoraInicioReserva())
                    .horaFinReserva(espacio.getHoraFinReserva())
                    .costoCobrado(espacio.getCostoCobrado())
                    .celular(espacio.getCelular())
                    .actividad(espacio.getActividad())
                    .maestro(espacio.getMaestro())
                    .build())
                    .collect(Collectors.toList());
        }

        // Mapear préstamos
        List<TicketPrestamoDTO> prestamosDTO = new ArrayList<>();
        if (ticket.getPrestamos() != null) {
            prestamosDTO = ticket.getPrestamos().stream()
                    .map(prestamo -> TicketPrestamoDTO.builder()
                    .prestamoId(prestamo.getId())
                    .productoId(prestamo.getProducto().getProductoId())
                    .productoNombre(prestamo.getProducto().getNombre())
                    .responsableId(prestamo.getResponsable().getUsuarioId())
                    .responsableNombre(prestamo.getResponsable().getNombreCompleto())
                    .fechaPrestamo(prestamo.getFechaPrestamo())
                    .fechaDevolucionEstimada(prestamo.getFechaDevolucionEstimada())
                    .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                    .estado(prestamo.getEstado() != null ? prestamo.getEstado().getDescripcion() : null)
                    .build())
                    .collect(Collectors.toList());
        }

        return TicketDTO.builder()
                .ticketId(ticket.getTicketId())
                .folio(ticket.getFolio())
                .creadoPorId(ticket.getCreadoPor().getUsuarioId())
                .creadoPorNombre(ticket.getCreadoPor().getNombreCompleto())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoFinanciero(ticket.getEstadoFinanciero())
                .ventas(ventasDTO)
                .cita(citaDTO)
                .espacios(espaciosDTO)
                .prestamos(prestamosDTO)
                .costoAdicional(ticket.getCostoAdicional())
                .motivoCostoAdicional(ticket.getMotivoCostoAdicional())
                .montoTotal(ticket.getMontoTotal())
                .montoPagado(ticket.getMontoPagado())
                .saldoPendiente(ticket.getSaldoPendiente())
                .cantidadPagos(ticket.getPagos() != null ? ticket.getPagos().size() : 0)
                .fechaCreacion(ticket.getFechaCreacion())
                .ultimaActualizacion(ticket.getUltimaActualizacion())
                .build();
    }
}
