package com.api_caich.service;

import com.api_caich.dto.reporte.DashboardAdminDTO;
import com.api_caich.dto.reporte.TicketReporteDTO;
import com.api_caich.entity.Ticket;
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
    private final PrestamoMaterialRepository prestamoRepository;
    private final ProductoRepository productoRepository;

    public DashboardAdminDTO obtenerEstadisticasAdmin() {
        // Citas hoy
        Long citasHoy = ticketRepository.countTicketsHoy();
        
        // Pagos pendientes
        List<Ticket> pagosPendientes = ticketRepository.findByEstadoPago("Pendiente");
        
        // Nuevos pacientes último mes
        Long nuevosPacientes = usuarioRepository.countNuevosPacientesUltimoMes();
        
        // Citas este mes
        Long citasEsteMes = ticketRepository.countTicketsEsteMes();
        
        // Préstamos activos y vencidos
        Long prestamosActivos = prestamoRepository.countByEstadoPrestamo("Activo");
        Long prestamosVencidos = prestamoRepository.countByEstadoPrestamo("Vencido");
        
        // Productos con stock bajo y sin stock
        //TODO: tipar estos list
        List<?> productosStockBajo = productoRepository.findProductosStockBajo();
        List<?> productosSinStock = productoRepository.findProductosSinStock();
        
        // Ingresos del mes (solo pagos verificados)
        BigDecimal ingresosMes = calcularIngresosMes();
        
        return DashboardAdminDTO.builder()
                .citasHoy(citasHoy != null ? citasHoy.intValue() : 0)
                .pagosPendientes(pagosPendientes != null ? pagosPendientes.size() : 0)
                .nuevosPacientes(nuevosPacientes != null ? nuevosPacientes.intValue() : 0)
                .citasEsteMes(citasEsteMes != null ? citasEsteMes.intValue() : 0)
                .prestamosActivos(prestamosActivos != null ? prestamosActivos.intValue() : 0)
                .prestamosVencidos(prestamosVencidos != null ? prestamosVencidos.intValue() : 0)
                .productosStockBajo(productosStockBajo != null ? productosStockBajo.size() : 0)
                .productosSinStock(productosSinStock != null ? productosSinStock.size() : 0)
                .ingresosMes(ingresosMes != null ? ingresosMes : BigDecimal.ZERO)
                .build();
    }

    public List<TicketReporteDTO> generarReporteIngresos(LocalDate fechaInicio, LocalDate fechaFin, 
                                                        Long terapeutaId, String estadoPago) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);
        
        List<Ticket> tickets = ticketRepository.findByFechaBetween(inicio, fin);
        
        // Aplicar filtros
        if (terapeutaId != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getTerapeuta().getUsuarioId().equals(terapeutaId))
                    .collect(Collectors.toList());
        }
        
        if (estadoPago != null) {
            tickets = tickets.stream()
                    .filter(t -> estadoPago.equals(t.getEstadoPago()))
                    .collect(Collectors.toList());
        }
        
        return tickets.stream()
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());
    }

    private BigDecimal calcularIngresosMes() {
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime finMes = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        List<Ticket> ticketsMes = ticketRepository.findByFechaBetween(inicioMes, finMes);
        
        return ticketsMes.stream()
                .filter(t -> "Verificado".equals(t.getEstadoPago()))
                .map(Ticket::getCostoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TicketReporteDTO mapToTicketReporteDTO(Ticket ticket) {
        return TicketReporteDTO.builder()
                .folio(ticket.getFolio())
                .fecha(ticket.getFecha())
                .pacienteNombre(ticket.getPaciente().getNombreCompleto())
                .terapeutaNombre(ticket.getTerapeuta().getNombreCompleto())
                .materia(ticket.getMateria())
                .costoTotal(ticket.getCostoTotal())
                .estadoTicket(ticket.getEstadoTicket())
                .estadoPago(ticket.getEstadoPago())
                .build();
    }
}