package com.api_caich.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.entity.MovimientoMonedero;
import com.api_caich.entity.Monedero;
import com.api_caich.entity.RecargaMonedero;
import com.api_caich.entity.SolicitudDevolucionMonedero;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.MonederoRepository;
import com.api_caich.repository.MovimientoMonederoRepository;
import com.api_caich.repository.RecargaMonederoRepository;
import com.api_caich.repository.SolicitudDevolucionMonederoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera reportes Excel del sistema de monedero virtual para contabilidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteFinancieroMonederoService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovimientoMonederoRepository movimientoRepository;
    private final RecargaMonederoRepository recargaRepository;
    private final MonederoRepository monederoRepository;
    private final SolicitudDevolucionMonederoRepository devolucionRepository;

    /**
     * Genera un libro Excel con 3 hojas:
     *  1. Movimientos     — todos los movimientos de saldo en el período
     *  2. Recargas        — todas las solicitudes de recarga (aprobadas/rechazadas/pendientes)
     *  3. Saldos Actuales — snapshot del saldo de cada alumno
     *
     * @param alumnoId  null = todos los alumnos
     * @param desde     fecha inicio (inclusive)
     * @param hasta     fecha fin (inclusive)
     */
    @Transactional(readOnly = true)
    public byte[] generarExcelCompleto(Long alumnoId, LocalDate desde, LocalDate hasta) {
        LocalDateTime dtDesde = (desde != null) ? desde.atStartOfDay() : LocalDate.of(2020, 1, 1).atStartOfDay();
        LocalDateTime dtHasta = (hasta != null) ? hasta.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

        List<MovimientoMonedero> movimientos;
        List<RecargaMonedero> recargas;

        if (alumnoId != null) {
            movimientos = movimientoRepository.findByAlumnoAndRangoFechas(alumnoId, dtDesde, dtHasta);
            recargas    = recargaRepository.findByAlumno_UsuarioIdOrderByFechaSolicitudDesc(alumnoId);
        } else {
            movimientos = movimientoRepository.findByRangoFechas(dtDesde, dtHasta);
            recargas    = recargaRepository.findByRangoFechas(dtDesde, dtHasta);
        }

        List<Monedero> saldos = monederoRepository.findAll();
        List<SolicitudDevolucionMonedero> devoluciones = devolucionRepository.findAll();

        try (Workbook wb = new XSSFWorkbook()) {
            crearHojaMovimientos(wb, movimientos);
            crearHojaRecargas(wb, recargas);
            crearHojaSaldos(wb, saldos);
            crearHojaDevoluciones(wb, devoluciones);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generando Excel de monedero", e);
            throw new BusinessException("Error al generar el reporte Excel: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOJA 1 — MOVIMIENTOS
    // ─────────────────────────────────────────────────────────────────────────

    private void crearHojaMovimientos(Workbook wb, List<MovimientoMonedero> movimientos) {
        Sheet sheet = wb.createSheet("Movimientos");

        String[] headers = {
            "ID", "Alumno", "Email", "Tipo", "Monto ($)", "Saldo Anterior ($)", "Saldo Nuevo ($)",
            "Descripción", "Ticket / Folio", "Fecha"
        };
        crearEncabezado(wb, sheet, headers, IndexedColors.DARK_BLUE);

        int rowNum = 1;
        BigDecimal totalRecargas     = BigDecimal.ZERO;
        BigDecimal totalConsumos     = BigDecimal.ZERO;
        BigDecimal totalDevoluciones = BigDecimal.ZERO;

        for (MovimientoMonedero m : movimientos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(m.getMovimientoId());
            row.createCell(1).setCellValue(m.getMonedero().getAlumno().getNombreCompleto());
            row.createCell(2).setCellValue(m.getMonedero().getAlumno().getEmail());
            row.createCell(3).setCellValue(m.getTipo().name());
            row.createCell(4).setCellValue(m.getMonto().doubleValue());
            row.createCell(5).setCellValue(m.getSaldoAnterior().doubleValue());
            row.createCell(6).setCellValue(m.getSaldoNuevo().doubleValue());
            row.createCell(7).setCellValue(m.getDescripcion() != null ? m.getDescripcion() : "");
            row.createCell(8).setCellValue(m.getTicket() != null ? m.getTicket().getFolio() : "");
            row.createCell(9).setCellValue(m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(DT_FMT) : "");

            switch (m.getTipo()) {
                case RECARGA -> totalRecargas     = totalRecargas.add(m.getMonto());
                case CONSUMO -> totalConsumos     = totalConsumos.add(m.getMonto());
                case DEVOLUCION_CANCELACION -> totalDevoluciones = totalDevoluciones.add(m.getMonto());
                default -> { /* AJUSTE_CREDITO: no se suma a totales operativos */ }
            }
        }

        // Fila de totales
        rowNum++;
        Row totals = sheet.createRow(rowNum);
        CellStyle boldStyle = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        boldStyle.setFont(bold);

        Cell label = totals.createCell(3);
        label.setCellValue("TOTALES");
        label.setCellStyle(boldStyle);
        totals.createCell(4).setCellValue(totalRecargas.add(totalDevoluciones).subtract(totalConsumos).doubleValue());

        rowNum++;
        Row r1 = sheet.createRow(rowNum++);
        r1.createCell(3).setCellValue("Total Recargas:");
        r1.createCell(4).setCellValue(totalRecargas.doubleValue());

        Row r2 = sheet.createRow(rowNum++);
        r2.createCell(3).setCellValue("Total Consumos:");
        r2.createCell(4).setCellValue(totalConsumos.doubleValue());

        Row r3 = sheet.createRow(rowNum);
        r3.createCell(3).setCellValue("Total Devoluciones:");
        r3.createCell(4).setCellValue(totalDevoluciones.doubleValue());

        autoSizeColumns(sheet, headers.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOJA 2 — RECARGAS
    // ─────────────────────────────────────────────────────────────────────────

    private void crearHojaRecargas(Workbook wb, List<RecargaMonedero> recargas) {
        Sheet sheet = wb.createSheet("Recargas");

        String[] headers = {
            "ID", "Alumno", "Email", "Folio Alumno", "Monto ($)", "Referencia Bancaria",
            "Estado", "Motivo Rechazo", "Verificado Por", "Fecha Solicitud", "Fecha Verificación", "Intento #"
        };
        crearEncabezado(wb, sheet, headers, IndexedColors.GREEN);

        int rowNum = 1;
        for (RecargaMonedero r : recargas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getRecargaMonederoId());
            row.createCell(1).setCellValue(r.getAlumno().getNombreCompleto());
            row.createCell(2).setCellValue(r.getAlumno().getEmail());
            row.createCell(3).setCellValue(r.getAlumno().getFolio() != null ? r.getAlumno().getFolio() : "");
            row.createCell(4).setCellValue(r.getMonto().doubleValue());
            row.createCell(5).setCellValue(r.getReferenciaBancaria() != null ? r.getReferenciaBancaria() : "");
            row.createCell(6).setCellValue(r.getEstado().name());
            row.createCell(7).setCellValue(r.getMotivoRechazo() != null ? r.getMotivoRechazo() : "");
            row.createCell(8).setCellValue(r.getVerificadoPor() != null ? r.getVerificadoPor().getNombreCompleto() : "");
            row.createCell(9).setCellValue(r.getFechaSolicitud() != null ? r.getFechaSolicitud().format(DT_FMT) : "");
            row.createCell(10).setCellValue(r.getFechaVerificacion() != null ? r.getFechaVerificacion().format(DT_FMT) : "");
            row.createCell(11).setCellValue(r.getNumeroIntento());
        }

        autoSizeColumns(sheet, headers.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOJA 3 — SALDOS ACTUALES
    // ─────────────────────────────────────────────────────────────────────────

    private void crearHojaSaldos(Workbook wb, List<Monedero> monederos) {
        Sheet sheet = wb.createSheet("Saldos Actuales");

        String[] headers = {
            "ID Monedero", "Alumno", "Email", "Folio Alumno", "ID Alumno", "Saldo Actual ($)", "Última Actualización"
        };
        crearEncabezado(wb, sheet, headers, IndexedColors.TEAL);

        BigDecimal totalSaldo = BigDecimal.ZERO;
        int rowNum = 1;

        for (Monedero m : monederos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(m.getMonederoId());
            row.createCell(1).setCellValue(m.getAlumno().getNombreCompleto());
            row.createCell(2).setCellValue(m.getAlumno().getEmail());
            row.createCell(3).setCellValue(m.getAlumno().getFolio() != null ? m.getAlumno().getFolio() : "");
            row.createCell(4).setCellValue(m.getAlumno().getUsuarioId());
            row.createCell(5).setCellValue(m.getSaldo().doubleValue());
            row.createCell(6).setCellValue(m.getUltimaActualizacion() != null ? m.getUltimaActualizacion().format(DT_FMT) : "");
            totalSaldo = totalSaldo.add(m.getSaldo());
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        CellStyle boldStyle = wb.createCellStyle();
        Font bold = wb.createFont(); bold.setBold(true); boldStyle.setFont(bold);
        Cell label = totalRow.createCell(4); label.setCellValue("TOTAL EN MONEDEROS:"); label.setCellStyle(boldStyle);
        Cell val = totalRow.createCell(5); val.setCellValue(totalSaldo.doubleValue()); val.setCellStyle(boldStyle);

        autoSizeColumns(sheet, headers.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOJA 4 — DEVOLUCIONES
    // ─────────────────────────────────────────────────────────────────────────

    private void crearHojaDevoluciones(Workbook wb, List<SolicitudDevolucionMonedero> devoluciones) {
        Sheet sheet = wb.createSheet("Devoluciones");

        String[] headers = {
            "ID", "Alumno", "Ticket Folio", "Monto Devolución ($)", "Estado",
            "Motivo Rechazo", "Procesado Por", "Fecha Solicitud", "Fecha Procesamiento"
        };
        crearEncabezado(wb, sheet, headers, IndexedColors.ORANGE);

        int rowNum = 1;
        for (SolicitudDevolucionMonedero d : devoluciones) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(d.getSolicitudDevolucionId());
            row.createCell(1).setCellValue(d.getMonedero().getAlumno().getNombreCompleto());
            row.createCell(2).setCellValue(d.getTicket().getFolio());
            row.createCell(3).setCellValue(d.getMontoDevolucion().doubleValue());
            row.createCell(4).setCellValue(d.getEstado().name());
            row.createCell(5).setCellValue(d.getMotivoRechazo() != null ? d.getMotivoRechazo() : "");
            row.createCell(6).setCellValue(d.getProcesadoPor() != null ? d.getProcesadoPor().getNombreCompleto() : "");
            row.createCell(7).setCellValue(d.getFechaSolicitud() != null ? d.getFechaSolicitud().format(DT_FMT) : "");
            row.createCell(8).setCellValue(d.getFechaProcesamiento() != null ? d.getFechaProcesamiento().format(DT_FMT) : "");
        }

        autoSizeColumns(sheet, headers.length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void crearEncabezado(Workbook wb, Sheet sheet, String[] headers, IndexedColors bgColor) {
        Row headerRow = sheet.createRow(0);
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            // Añadir un poco de padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }
    }
}
