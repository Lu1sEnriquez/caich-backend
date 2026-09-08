package com.api_caich.service;

import com.api_caich.dto.reporte.DashboardAdminDTO;
import com.api_caich.dto.reporte.TicketReporteDTO;
import com.api_caich.entity.Pago;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.EstadoPrestamo;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

            @Transactional(readOnly = true)
            public byte[] exportUsuariosExcel(String rol, String estado, String search) {
            List<Usuario> usuarios = obtenerUsuariosParaExport(rol, estado, search);

            String[] headers = {
                "ID",
                "Nombre",
                "Email",
                "Rol",
                "Estado",
                "Telefono",
                "Folio",
                "Id Alumno",
                "Fecha Creacion",
                "Ultima Actualizacion"
            };

            List<List<Object>> rows = new ArrayList<>();
            for (Usuario usuario : usuarios) {
                rows.add(Arrays.asList(
                    usuario.getUsuarioId(),
                    usuario.getNombreCompleto(),
                    usuario.getEmail(),
                    usuario.getRol(),
                    usuario.getEstado(),
                    usuario.getTelefono(),
                    usuario.getFolio(),
                    usuario.getIdAlumno(),
                    usuario.getFechaCreacion(),
                    usuario.getUltimaActualizacion()
                ));
            }

            return buildWorkbook("Usuarios", headers, rows);
            }

            @Transactional(readOnly = true)
            public byte[] exportProductosExcel(
                String nombre,
                String categoria,
                Boolean estaActivo,
                Boolean esPrestable,
                Boolean esVendible) {

            Page<Producto> page = productoRepository.findAllConFiltros(
                nombre, categoria, estaActivo, esPrestable, esVendible, Pageable.unpaged());
            List<Producto> productos = page.getContent();

            String[] headers = {
                "ID",
                "Codigo",
                "Nombre",
                "Categoria",
                "Precio",
                "Stock",
                "Stock Minimo",
                "Vendible",
                "Prestable",
                "Activo",
                "Descripcion"
            };

            List<List<Object>> rows = new ArrayList<>();
            for (Producto producto : productos) {
                rows.add(Arrays.asList(
                    producto.getProductoId(),
                    producto.getCodigo(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getPrecio(),
                    producto.getStock(),
                    producto.getStockMinimo(),
                    producto.getEsVendible(),
                    producto.getEsPrestable(),
                    producto.getEstaActivo(),
                    producto.getDescripcion()
                ));
            }

            return buildWorkbook("Productos", headers, rows);
            }

            @Transactional(readOnly = true)
            public byte[] exportTicketsExcel(
                Long pacienteId,
                Long terapeutaId,
                com.api_caich.enums.EstadoTicket estadoTicket,
                EstadoFinanciero estadoFinanciero,
                LocalDateTime fechaInicio,
                LocalDateTime fechaFin) {

            Page<Ticket> page = ticketRepository.findWithFilters(
                pacienteId, terapeutaId, estadoTicket, estadoFinanciero,
                fechaInicio, fechaFin, Pageable.unpaged());
            List<TicketReporteDTO> reporte = page.getContent().stream()
                .map(this::mapToTicketReporteDTO)
                .collect(Collectors.toList());

            String[] headers = {
                "Folio",
                "Fecha Creacion",
                "Fecha Cita",
                "Paciente",
                "Terapeuta",
                "Monto Total",
                "Monto Pagado",
                "Saldo Pendiente",
                "Estado Ticket",
                "Estado Financiero"
            };

            List<List<Object>> rows = new ArrayList<>();
            for (TicketReporteDTO ticket : reporte) {
                rows.add(Arrays.asList(
                    ticket.getFolio(),
                    ticket.getFechaCreacion(),
                    ticket.getFechaInicioCita(),
                    ticket.getPacienteNombre(),
                    ticket.getTerapeutaNombre(),
                    ticket.getMontoTotal(),
                    ticket.getMontoPagado(),
                    ticket.getSaldoPendiente(),
                    ticket.getEstadoTicket(),
                    ticket.getEstadoFinanciero()
                ));
            }

            return buildWorkbook("Tickets", headers, rows);
            }

            @Transactional(readOnly = true)
            public byte[] exportPagosExcel(EstadoPagoTransaccion estado, Long ticketId) {
            List<Pago> pagos;

            if (ticketId != null) {
                pagos = pagoRepository.findByTicket_TicketId(ticketId);
                if (estado != null) {
                pagos = pagos.stream()
                    .filter(p -> estado.equals(p.getEstado()))
                    .collect(Collectors.toList());
                }
            } else if (estado != null) {
                pagos = pagoRepository.findByEstado(estado);
            } else {
                pagos = pagoRepository.findAll();
            }

            pagos = pagos.stream()
                .filter(p -> p.getMetodoPago() != com.api_caich.enums.MetodoPago.MONEDERO)
                .collect(Collectors.toList());

            String[] headers = {
                "ID",
                "Folio Ticket",
                "Monto",
                "Metodo",
                "Estado",
                "Referencia",
                "Fecha Pago",
                "Verificado Por",
                "Comprobante"
            };

            List<List<Object>> rows = new ArrayList<>();
            for (Pago pago : pagos) {
                rows.add(Arrays.asList(
                    pago.getPagoId(),
                    pago.getTicket() != null ? pago.getTicket().getFolio() : null,
                    pago.getMonto(),
                    pago.getMetodoPago(),
                    pago.getEstado(),
                    pago.getReferencia(),
                    pago.getFechaPago(),
                    pago.getVerificadoPor() != null ? pago.getVerificadoPor().getNombreCompleto() : null,
                    pago.getComprobanteUrl()
                ));
            }

            return buildWorkbook("Pagos", headers, rows);
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

    private List<Usuario> obtenerUsuariosParaExport(String rol, String estado, String search) {
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch) {
            List<Usuario> result = usuarioRepository.searchUsuarios(search, Pageable.unpaged()).getContent();
            return result.stream()
                    .filter(u -> rol == null || rol.isBlank() || rol.equalsIgnoreCase(u.getRol()))
                    .filter(u -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(u.getEstado()))
                    .collect(Collectors.toList());
        }

        if (rol != null && !rol.isBlank() && estado != null && !estado.isBlank()) {
            return usuarioRepository.findByRolAndEstado(rol, estado);
        }
        if (rol != null && !rol.isBlank()) {
            return usuarioRepository.findByRol(rol);
        }
        if (estado != null && !estado.isBlank()) {
            return usuarioRepository.findByEstado(estado);
        }
        return usuarioRepository.findAll();
    }

    private byte[] buildWorkbook(String sheetName, String[] headers, List<List<Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    setCellValue(cell, rowData.get(i));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el archivo Excel");
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.format(DATE_TIME_FORMATTER));
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
