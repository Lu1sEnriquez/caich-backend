package com.api_caich.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.reporte.DashboardAdminDTO;
import com.api_caich.dto.reporte.TicketReporteDTO;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.service.ReporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "APIs for generating reports and statistics")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final ReporteService reporteService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard", description = "Get statistics for admin dashboard")
    public ResponseEntity<DataResponseDTO<DashboardAdminDTO>> obtenerDashboardAdmin() {
        DashboardAdminDTO dashboard = reporteService.obtenerEstadisticasAdmin();
        return ResponseEntity.ok(DataResponseDTO.success(dashboard, "Dashboard statistics retrieved"));
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate income report", description = "Generate income report with filters")
    public ResponseEntity<DataResponseDTO<List<TicketReporteDTO>>> generarReporteIngresos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long terapeutaId,
            @RequestParam(required = false) String estadoPago) {

        List<TicketReporteDTO> reporte = reporteService.generarReporteIngresos(fechaInicio, fechaFin, terapeutaId,
                estadoPago);
        return ResponseEntity.ok(DataResponseDTO.success(reporte, "Income report generated"));
    }

        @GetMapping("/excel/usuarios")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Export users to Excel", description = "Export users to Excel with optional filters")
        public ResponseEntity<byte[]> exportUsuariosExcel(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String search) {

        byte[] excel = reporteService.exportUsuariosExcel(rol, estado, search);
        return buildExcelResponse(excel, buildFileName("usuarios"));
        }

        @GetMapping("/excel/productos")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Export products to Excel", description = "Export products to Excel with optional filters")
        public ResponseEntity<byte[]> exportProductosExcel(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean estaActivo,
            @RequestParam(required = false) Boolean esPrestable,
            @RequestParam(required = false) Boolean esVendible) {

        String nombreFinal = (nombre == null || nombre.isBlank()) ? search : nombre;
        byte[] excel = reporteService.exportProductosExcel(
            nombreFinal, categoria, estaActivo, esPrestable, esVendible);
        return buildExcelResponse(excel, buildFileName("productos"));
        }

        @GetMapping("/excel/tickets")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Export tickets to Excel", description = "Export tickets to Excel with optional filters")
        public ResponseEntity<byte[]> exportTicketsExcel(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long terapeutaId,
            @RequestParam(required = false) EstadoTicket estadoTicket,
            @RequestParam(required = false) EstadoFinanciero estadoFinanciero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timeEnd) {

        LocalDateTime fechaInicioDateTime = timeStart != null ? timeStart
            : (fechaInicio != null ? fechaInicio.atStartOfDay() : null);
        LocalDateTime fechaFinDateTime = timeEnd != null ? timeEnd
            : (fechaFin != null ? fechaFin.atTime(23, 59, 59) : null);

        byte[] excel = reporteService.exportTicketsExcel(
            pacienteId,
            terapeutaId,
            estadoTicket,
            estadoFinanciero,
            fechaInicioDateTime,
            fechaFinDateTime);
        return buildExcelResponse(excel, buildFileName("tickets"));
        }

        @GetMapping("/excel/pagos")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Export payments to Excel", description = "Export payments to Excel with optional filters")
        public ResponseEntity<byte[]> exportPagosExcel(
            @RequestParam(required = false) EstadoPagoTransaccion estado,
            @RequestParam(required = false) Long ticketId) {

        byte[] excel = reporteService.exportPagosExcel(estado, ticketId);
        return buildExcelResponse(excel, buildFileName("pagos"));
        }

        private ResponseEntity<byte[]> buildExcelResponse(byte[] excel, String filename) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excel);
        }

        private String buildFileName(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(FILE_TS) + ".xlsx";
        }
}
