package com.api_caich.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.monedero.MonederoDTO;
import com.api_caich.dto.monedero.MovimientoMonederoDTO;
import com.api_caich.dto.monedero.RecargaMonederoDTO;
import com.api_caich.dto.monedero.RechazarRequest;
import com.api_caich.dto.monedero.SolicitudDevolucionDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.MonederoService;
import com.api_caich.service.ReporteFinancieroMonederoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints del sistema de monedero para el Administrador.
 * Finanzas / Contabilidad: recargas, devoluciones, movimientos, reportes Excel.
 */
@RestController
@RequestMapping("/admin/monedero")
@RequiredArgsConstructor
    @PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Monedero", description = "Finance management for administrators")
@SecurityRequirement(name = "bearerAuth")
public class MonederoAdminController {

    private final MonederoService monederoService;
    private final ReporteFinancieroMonederoService reporteService;

    // ─── MONEDEROS (SALDOS) ───────────────────────────────────────────────────

    @GetMapping("/monederos")
    @Operation(summary = "Lista todos los monederos con saldo actual de cada alumno")
    public ResponseEntity<DataResponseDTO<List<MonederoDTO>>> obtenerTodosLosMonederos() {
        return ResponseEntity.ok(DataResponseDTO.success(
                monederoService.obtenerTodosLosMonederos(), "Monederos obtenidos"));
    }

    // ─── RECARGAS ─────────────────────────────────────────────────────────────

    @GetMapping("/recargas/pendientes")
    @Operation(summary = "Lista recargas pendientes de revisión")
    public ResponseEntity<DataResponseDTO<List<RecargaMonederoDTO>>> obtenerRecargasPendientes() {
        return ResponseEntity.ok(DataResponseDTO.success(
                monederoService.obtenerRecargasPendientes(), "Recargas pendientes obtenidas"));
    }

    @GetMapping("/recargas")
    @Operation(summary = "Lista todas las recargas (paginado, todas las páginas)")
    public ResponseEntity<DataResponseDTO<Page<RecargaMonederoDTO>>> obtenerTodasLasRecargas(
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(DataResponseDTO.success(
                monederoService.obtenerTodasLasRecargas(pageable), "Recargas obtenidas"));
    }

    @PostMapping("/recargas/{recargaId}/aprobar")
    @Operation(summary = "Aprobar una recarga: acredita el saldo al monedero del alumno")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> aprobarRecarga(
            @PathVariable Long recargaId,
            @AuthenticationPrincipal CustomUserDetails admin) {
        RecargaMonederoDTO dto = monederoService.aprobarRecarga(recargaId, admin.getId());
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Recarga aprobada y saldo acreditado"));
    }

    @PostMapping("/recargas/{recargaId}/rechazar")
    @Operation(summary = "Rechazar una recarga con motivo obligatorio")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> rechazarRecarga(
            @PathVariable Long recargaId,
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestBody @Validated RechazarRequest request) {
        RecargaMonederoDTO dto = monederoService.rechazarRecarga(recargaId, admin.getId(), request);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Recarga rechazada"));
    }

    // ─── DEVOLUCIONES ─────────────────────────────────────────────────────────

    @GetMapping("/devoluciones/pendientes")
    @Operation(summary = "Lista solicitudes de devolución pendientes de aprobación")
    public ResponseEntity<DataResponseDTO<List<SolicitudDevolucionDTO>>> obtenerDevolucionesPendientes() {
        return ResponseEntity.ok(DataResponseDTO.success(
                monederoService.obtenerDevolucionesPendientes(), "Devoluciones pendientes obtenidas"));
    }

    @GetMapping("/devoluciones")
    @Operation(summary = "Lista todas las solicitudes de devolución")
    public ResponseEntity<DataResponseDTO<List<SolicitudDevolucionDTO>>> obtenerTodasLasDevoluciones() {
        return ResponseEntity.ok(DataResponseDTO.success(
                monederoService.obtenerTodasLasDevoluciones(), "Devoluciones obtenidas"));
    }

    @PostMapping("/devoluciones/{solicitudId}/aprobar")
    @Operation(summary = "Aprobar devolución: reintegra el monto al monedero del alumno")
    public ResponseEntity<DataResponseDTO<SolicitudDevolucionDTO>> aprobarDevolucion(
            @PathVariable Long solicitudId,
            @AuthenticationPrincipal CustomUserDetails admin) {
        SolicitudDevolucionDTO dto = monederoService.aprobarDevolucion(solicitudId, admin.getId());
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Devolución aprobada y saldo reintegrado"));
    }

    @PostMapping("/devoluciones/{solicitudId}/rechazar")
    @Operation(summary = "Rechazar devolución con motivo obligatorio")
    public ResponseEntity<DataResponseDTO<SolicitudDevolucionDTO>> rechazarDevolucion(
            @PathVariable Long solicitudId,
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestBody @Validated RechazarRequest request) {
        SolicitudDevolucionDTO dto = monederoService.rechazarDevolucion(solicitudId, admin.getId(), request);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Solicitud de devolución rechazada"));
    }

    // ─── MOVIMIENTOS ─────────────────────────────────────────────────────────

    @GetMapping("/movimientos")
    @Operation(summary = "Lista movimientos de monedero con filtros opcionales")
    public ResponseEntity<DataResponseDTO<List<MovimientoMonederoDTO>>> obtenerMovimientos(
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<MovimientoMonederoDTO> lista = monederoService.obtenerMovimientosAdmin(alumnoId, desde, hasta);
        return ResponseEntity.ok(DataResponseDTO.success(lista, "Movimientos obtenidos"));
    }

    // ─── CONTADORES PARA BADGES ───────────────────────────────────────────────

    @GetMapping("/contadores")
    @Operation(summary = "Retorna contadores de pendientes para mostrar en badges del sidebar")
    public ResponseEntity<DataResponseDTO<java.util.Map<String, Long>>> obtenerContadores() {
        java.util.Map<String, Long> contadores = java.util.Map.of(
                "recargasPendientes", monederoService.contarRecargasPendientes(),
                "devolucionesPendientes", monederoService.contarDevolucionesPendientes()
        );
        return ResponseEntity.ok(DataResponseDTO.success(contadores, "Contadores obtenidos"));
    }

    // ─── REPORTE EXCEL ────────────────────────────────────────────────────────

    @GetMapping("/reporte/excel")
    @Operation(summary = "Exportar reporte financiero completo en Excel (.xlsx)")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        byte[] excelBytes = reporteService.generarExcelCompleto(alumnoId, desde, hasta);

        String filename = "reporte-monedero-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
