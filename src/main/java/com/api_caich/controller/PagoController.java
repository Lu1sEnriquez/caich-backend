package com.api_caich.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.pago.PagoDTO;
import com.api_caich.dto.pago.PagoMultipartRequestDTO;
import com.api_caich.dto.pago.PagoRequestDTO;
import com.api_caich.dto.pago.SincronizacionAjustesRequestDTO;
import com.api_caich.dto.pago.SincronizacionPreviewDTO;
import com.api_caich.dto.pago.SincronizacionResultadoDTO;
import com.api_caich.entity.Pago;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.FileStorageService;
import com.api_caich.service.GoogleFormsSyncService;
import com.api_caich.service.PagoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "APIs for payment management")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoService pagoService;
    private final FileStorageService fileStorageService;
    private final GoogleFormsSyncService googleFormsSyncService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Register payment", description = "Register a new payment for a ticket")
    public ResponseEntity<DataResponseDTO<PagoDTO>> registrarPago(
            @Valid @RequestBody PagoRequestDTO pagoRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.registrarPago(pagoRequest, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment registered successfully"));
    }

    @PutMapping("/{pagoId}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve payment", description = "Approve and verify a payment")
    public ResponseEntity<DataResponseDTO<PagoDTO>> aprobarPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.aprobarPago(pagoId, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment approved successfully"));
    }

    @PutMapping("/{pagoId}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject payment", description = "Reject a payment with reason")
    public ResponseEntity<DataResponseDTO<PagoDTO>> rechazarPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId,
            @RequestParam String motivo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.rechazarPago(pagoId, motivo, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment rejected successfully"));
    }

    @PutMapping("/{pagoId}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel payment", description = "Cancel an approved payment with reason")
    public ResponseEntity<DataResponseDTO<PagoDTO>> cancelarPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId,
            @RequestParam String motivo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Pago pago = pagoService.cancelarPago(pagoId, motivo, userDetails.getId());
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment cancelled successfully"));
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'PACIENTE')")
    @Operation(summary = "Get payments for ticket", description = "Get all payments associated with a specific ticket")
    public ResponseEntity<DataResponseDTO<List<PagoDTO>>> obtenerPagosDeTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {

        List<Pago> pagos = pagoService.obtenerPagosDeTicket(ticketId);
        List<PagoDTO> pagoDTOs = pagos.stream()
                .map(PagoDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTOs, "Payments retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments", description = "Get paginated list of all payments (admin only)")
    public ResponseEntity<DataResponseDTO<Page<PagoDTO>>> obtenerTodosPagos(
            @Parameter(description = "Payment status") @RequestParam(required = false) EstadoPagoTransaccion estado,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "fechaPago") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Pago> pagos = estado != null
                ? pagoService.obtenerPagosConEstado(estado, pageable)
                : pagoService.obtenerTodosPagos(pageable);

        Page<PagoDTO> pagoDTOs = pagos.map(PagoDTO::fromEntity);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTOs, "Payments retrieved successfully"));
    }

    @GetMapping("/{pagoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Get payment by ID", description = "Get details of a specific payment")
    public ResponseEntity<DataResponseDTO<PagoDTO>> obtenerPago(
            @Parameter(description = "Payment ID") @PathVariable Long pagoId) {

        Pago pago = pagoService.obtenerPago(pagoId);
        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(pagoDTO, "Payment retrieved successfully"));
    }

    @GetMapping("/comprobantes/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Get payment receipt", description = "Get receipt file (image/PDF)")
    public ResponseEntity<Resource> obtenerComprobante(
            @Parameter(description = "Receipt file name") @PathVariable String filename) {

        Resource resource = fileStorageService.cargarArchivo("comprobantes", filename);
        String contentType = "application/octet-stream";

        try {
            String detected = Files.probeContentType(resource.getFile().toPath());
            if (detected != null && !detected.isBlank()) {
                contentType = detected;
            }
        } catch (IOException ex) {
            // Fallback to default content type
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // ── Nuevos endpoints con soporte multipart ──

    @PostMapping(value = "/subir-comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PACIENTE')")
    @Operation(summary = "Upload payment with receipt",
               description = "Register a payment uploading a receipt file (image/PDF). Sets status to PENDIENTE_REVISION.")
    public ResponseEntity<DataResponseDTO<PagoDTO>> subirComprobante(
            @RequestPart("pago") @Valid PagoMultipartRequestDTO pagoRequest,
            @RequestPart("comprobante") MultipartFile comprobante,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Guardar el archivo del comprobante
        String comprobanteUrl = fileStorageService.almacenarComprobante(comprobante, pagoRequest.getTicketId());

        // Registrar el pago
        Pago pago = pagoService.registrarPagoConComprobante(
                pagoRequest.getTicketId(),
                pagoRequest.getMonto(),
                pagoRequest.getMetodoPago().name(),
                pagoRequest.getReferencia(),
                pagoRequest.getNotas(),
                comprobanteUrl,
                userDetails.getId());

        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(
                pagoDTO, "Pago registrado con comprobante. Pendiente de verificación."));
    }

    @PostMapping(value = "/registrar-manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register manual payment (admin)",
               description = "Admin registers a payment directly as APROBADO. Receipt file is optional.")
    public ResponseEntity<DataResponseDTO<PagoDTO>> registrarPagoManual(
            @RequestPart("pago") @Valid PagoMultipartRequestDTO pagoRequest,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String comprobanteUrl = null;
        if (comprobante != null && !comprobante.isEmpty()) {
            comprobanteUrl = fileStorageService.almacenarComprobante(comprobante, pagoRequest.getTicketId());
        }

        Pago pago = pagoService.registrarPagoManual(
                pagoRequest.getTicketId(),
                pagoRequest.getMonto(),
                pagoRequest.getMetodoPago().name(),
                pagoRequest.getReferencia(),
                pagoRequest.getNotas(),
                comprobanteUrl,
                userDetails.getId());

        PagoDTO pagoDTO = PagoDTO.fromEntity(pago);

        return ResponseEntity.ok(DataResponseDTO.success(
                pagoDTO, "Pago manual registrado y aprobado."));
    }

    @PostMapping("/sincronizar-forms/preview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Previsualizar sincronización de Google Forms",
        description = "Analiza el CSV de Google Forms y retorna un preview detallado de qué pagos se pueden sincronizar y cuáles tienen errores, sin guardar nada en la BD"
    )
    public ResponseEntity<DataResponseDTO<SincronizacionPreviewDTO>> previsualizarSincronizacion() {
        SincronizacionPreviewDTO preview = googleFormsSyncService.previsualizarSincronizacion();
        
        String mensaje = String.format(
            "Preview generado: %d válidos, %d con errores, %d duplicados de %d registros totales",
            preview.getTotalValidos(),
            preview.getTotalErrores(),
            preview.getTotalDuplicados(),
            preview.getTotalRegistros()
        );
        
        return ResponseEntity.ok(DataResponseDTO.success(preview, mensaje));
    }

    @PostMapping("/sincronizar-forms/preview-ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Previsualizar sincronización con ajustes",
        description = "Analiza el CSV aplicando ajustes temporales hechos en el review para mostrar un preview actualizado sin guardar nada en BD"
    )
    public ResponseEntity<DataResponseDTO<SincronizacionPreviewDTO>> previsualizarSincronizacionConAjustes(
            @RequestBody(required = false) SincronizacionAjustesRequestDTO request) {

        SincronizacionAjustesRequestDTO payload = request == null
                ? SincronizacionAjustesRequestDTO.builder().build()
                : request;

        SincronizacionPreviewDTO preview = googleFormsSyncService.previsualizarSincronizacionConAjustes(payload.getAjustes());

        String mensaje = String.format(
            "Preview con ajustes: %d válidos, %d con errores, %d duplicados de %d registros totales",
            preview.getTotalValidos(),
            preview.getTotalErrores(),
            preview.getTotalDuplicados(),
            preview.getTotalRegistros()
        );

        return ResponseEntity.ok(DataResponseDTO.success(preview, mensaje));
    }

    @PostMapping("/sincronizar-forms/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Confirmar y ejecutar sincronización",
        description = "Ejecuta la sincronización de los pagos válidos detectados en el preview. Solo sincroniza registros sin errores."
    )
    public ResponseEntity<DataResponseDTO<SincronizacionResultadoDTO>> confirmarSincronizacion(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        SincronizacionResultadoDTO resultado = googleFormsSyncService.confirmarSincronizacion(userDetails.getId());
        
        return ResponseEntity.ok(DataResponseDTO.success(resultado, resultado.getMensaje()));
    }

    @PostMapping("/sincronizar-forms/confirmar-ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Confirmar y ejecutar sincronización con ajustes",
        description = "Ejecuta la sincronización aplicando ajustes temporales del review. Solo sincroniza registros válidos después de validar los ajustes."
    )
    public ResponseEntity<DataResponseDTO<SincronizacionResultadoDTO>> confirmarSincronizacionConAjustes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) SincronizacionAjustesRequestDTO request) {

        SincronizacionAjustesRequestDTO payload = request == null
                ? SincronizacionAjustesRequestDTO.builder().build()
                : request;

        SincronizacionResultadoDTO resultado = googleFormsSyncService
                .confirmarSincronizacionConAjustes(userDetails.getId(), payload.getAjustes());

        return ResponseEntity.ok(DataResponseDTO.success(resultado, resultado.getMensaje()));
    }
}
