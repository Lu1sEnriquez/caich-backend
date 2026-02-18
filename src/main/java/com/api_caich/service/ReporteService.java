package com.api_caich.service;

import com.api_caich.dto.reporte.DashboardAdminDTO;
import com.api_caich.dto.reporte.TicketReporteDTO;
import com.api_caich.entity.Ticket;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPrestamo;
import com.api_caich.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketPrestamoRepository ticketPrestamoRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;

    public DashboardAdminDTO obtenerEstadisticasAdmin() {
        // Obtener todas las citas de hoy
        int citasHoy = (int) ticketRepository.findAll().stream()
                .filter(t -> t.getCita() != null &&
                       t.getCita().getFechaInicio().toLocalDate().equals(LocalDate.now()))
                .count();

        // Contar pagos pendientes
        int pagosPendientes = (int) ticketRepository.findAll().stream()
                .filter(t -> EstadoFinanciero.PENDIENTE.equals(t.getEstadoFinanciero()))
                .count();

        // Nuevos pacientes este mes
        int nuevosPacientes = (int) usuarioRepository.findAll().stream()
                .filter(u -> u.getFechaCreacion() != null && 
                       u.getFechaCreacion().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();

        // Citas este mes
        int citasEsteMes = (int) ticketRepository.findAll().stream()
                .filter(t -> t.getCita() != null &&
                       t.getCita().getFechaInicio().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();

        // Préstamos activos
        int prestamosActivos = (int) ticketPrestamoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null && p.getEstado().equals(EstadoPrestamo.ACTIVO))
                .count();

        // Préstamos vencidos
        int prestamosVencidos = (int) ticketPrestamoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null && p.getEstado().equals(EstadoPrestamo.VENCIDO))
                .count();

        // Productos con stock bajo (asumiendo hay un campo stock < 10)
        int productosStockBajo = (int) productoRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() < 10)
                .count();

        // Productos sin stock
        int productosSinStock = (int) productoRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() == 0)
                .count();

        // Ingresos del mes
        BigDecimal ingresosMes = calcularIngresosMes();

        return DashboardAdminDTO.builder()
                .citasHoy(citasHoy)
                .pagosPendientes(pagosPendientes)
                .nuevosPacientes(nuevosPacientes)
                .citasEsteMes(citasEsteMes)
                .prestamosActivos(prestamosActivos)
                .prestamosVencidos(prestamosVencidos)
                .productosStockBajo(productosStockBajo)
                .productosSinStock(productosSinStock)
                .ingresosMes(ingresosMes)
                .build();
    }

    public List<TicketReporteDTO> obtenerReporteTickets(String estado, String fechaInicio, String fechaFin) {
        List<Ticket> tickets = ticketRepository.findAll();

        // Filtrar por estado si se proporciona
        if (estado != null && !estado.isEmpty()) {
            try {
                EstadoFinanciero estadoEnum = EstadoFinanciero.valueOf(estado.toUpperCase());
                tickets = tickets.stream()
                        .filter(t -> estadoEnum.equals(t.getEstadoFinanciero()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Estado inválido, continuar sin filtrar
            }
        }

        return tickets.stream()
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());
    }

    public List<TicketReporteDTO> generarReporteIngresos(LocalDate fechaInicio, LocalDate fechaFin, Long terapeutaId, String estadoPago) {
        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .filter(t -> {
                    boolean fechaOk = true;
                    if (fechaInicio != null && fechaFin != null) {
                        LocalDateTime inicio = fechaInicio.atStartOfDay();
                        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay();
                        fechaOk = t.getFechaCreacion().isAfter(inicio) && t.getFechaCreacion().isBefore(fin);
                    }
                    
                    boolean terapeutaOk = true;
                    if (terapeutaId != null && t.getCita() != null) {
                        terapeutaOk = t.getCita().getTerapeuta() != null && 
                                     t.getCita().getTerapeuta().getUsuarioId().equals(terapeutaId);
                    }
                    
                    boolean estadoOk = true;
                    if (estadoPago != null && !estadoPago.isEmpty()) {
                        try {
                            EstadoFinanciero estado = EstadoFinanciero.valueOf(estadoPago.toUpperCase());
                            estadoOk = estado.equals(t.getEstadoFinanciero());
                        } catch (IllegalArgumentException e) {
                            estadoOk = false;
                        }
                    }
                    
                    return fechaOk && terapeutaOk && estadoOk;
                })
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());
    }

    public List<TicketReporteDTO> obtenerInformePagos() {
        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .filter(t -> EstadoFinanciero.PAGADO.equals(t.getEstadoFinanciero()) || 
                           EstadoFinanciero.PAGO_PARCIAL.equals(t.getEstadoFinanciero()))
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());
    }

    public List<TicketReporteDTO> obtenerInformeNotaPaciente(Long pacienteId) {
        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .filter(t -> t.getCita() != null && 
                           t.getCita().getPaciente() != null &&
                           t.getCita().getPaciente().getUsuarioId().equals(pacienteId))
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());
    }

    private BigDecimal calcularIngresosMes() {
        try {
            return pagoRepository.findAll().stream()
                    .filter(p -> p.getFechaPago() != null &&
                               p.getFechaPago().isAfter(LocalDateTime.now().minusMonths(1)))
                    .map(p -> p.getMonto())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private TicketReporteDTO mapToTicketReporteDTO(Ticket ticket) {
        return TicketReporteDTO.builder()
                .folio(ticket.getFolio())
                .fechaCreacion(ticket.getFechaCreacion())
                .fechaInicioCita(ticket.getCita() != null ? ticket.getCita().getFechaInicio() : null)
                .pacienteNombre(ticket.getCita() != null && ticket.getCita().getPaciente() != null ?
                        ticket.getCita().getPaciente().getNombreCompleto() : "S/N")
                .terapeutaNombre(ticket.getCita() != null && ticket.getCita().getTerapeuta() != null ?
                        ticket.getCita().getTerapeuta().getNombreCompleto() : "S/N")
                .montoTotal(ticket.getMontoTotal())
                .montoPagado(ticket.getMontoPagado())
                .saldoPendiente(ticket.getSaldoPendiente())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoFinanciero(ticket.getEstadoFinanciero())
                .build();
    }
}
