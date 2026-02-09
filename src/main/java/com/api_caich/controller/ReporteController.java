package com.api_caich.controller;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.reporte.DashboardAdminDTO;
import com.api_caich.dto.reporte.TicketReporteDTO;
import com.api_caich.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "APIs for generating reports and statistics")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

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
}