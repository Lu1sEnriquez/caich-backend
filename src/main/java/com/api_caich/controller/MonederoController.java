
package com.api_caich.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.monedero.CrearRecargaMonederoRequest;
import com.api_caich.dto.monedero.MonederoDTO;
import com.api_caich.dto.monedero.MovimientoMonederoDTO;
import com.api_caich.dto.monedero.RecargaMonederoDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.MonederoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints del monedero para el alumno autenticado.
 * Todos los métodos obtienen el alumnoId del JWT — nunca del body/path,
 * para prevenir que un alumno manipule datos de otro usuario.
 */
@RestController
@RequestMapping("/monedero")
@RequiredArgsConstructor
@Tag(name = "Monedero", description = "Wallet management for students")
@SecurityRequirement(name = "bearerAuth")
public class MonederoController {

    private final MonederoService monederoService;

    // ─── SALDO ───────────────────────────────────────────────────────────────

    @GetMapping("/mi-saldo")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Obtener mi monedero y saldo actual")
    public ResponseEntity<DataResponseDTO<MonederoDTO>> obtenerMiMonedero(
            @AuthenticationPrincipal CustomUserDetails user) {
        MonederoDTO dto = monederoService.obtenerMonedero(user.getId());
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Monedero obtenido"));
    }

    // ─── MOVIMIENTOS ─────────────────────────────────────────────────────────

    @GetMapping("/mis-movimientos")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Historial de movimientos paginado del usuario")
    public ResponseEntity<DataResponseDTO<Page<MovimientoMonederoDTO>>> obtenerMisMovimientos(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20, sort = "fechaMovimiento", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MovimientoMonederoDTO> page = monederoService.obtenerMisMovimientos(user.getId(), pageable);
        return ResponseEntity.ok(DataResponseDTO.success(page, "Movimientos obtenidos"));
    }

    // ─── RECARGAS ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/recargas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Solicitar recarga de saldo (subir comprobante)")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> solicitarRecarga(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("datos") @Validated CrearRecargaMonederoRequest request,
            @RequestPart("comprobante") MultipartFile comprobante) {
        RecargaMonederoDTO dto = monederoService.solicitarRecarga(user.getId(), request, comprobante);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Solicitud de recarga enviada. Pendiente de revisión."));
    }

    @PostMapping(value = "/recargas/{recargaId}/reintentar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Reintentar una recarga rechazada con comprobante corregido")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> reintentarRecarga(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId,
            @RequestPart("datos") @Validated CrearRecargaMonederoRequest request,
            @RequestPart("comprobante") MultipartFile comprobante) {
        RecargaMonederoDTO dto = monederoService.reintentarRecarga(user.getId(), recargaId, request, comprobante);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Solicitud de recarga reenviada."));
    }

    @GetMapping("/mis-recargas")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Historial de solicitudes de recarga del usuario")
    public ResponseEntity<DataResponseDTO<List<RecargaMonederoDTO>>> obtenerMisRecargas(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<RecargaMonederoDTO> lista = monederoService.obtenerMisRecargas(user.getId());
        return ResponseEntity.ok(DataResponseDTO.success(lista, "Recargas obtenidas"));
    }

    // ─── RECARGA POR TESORERÍA ─────────────────────────────────────────────

    @PostMapping("/recargas/tesoreria")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Solicitar recarga por tesorería (genera folio y ticket)")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> solicitarRecargaTesoreria(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("datos") @Validated CrearRecargaMonederoRequest request) {
        RecargaMonederoDTO dto = monederoService.solicitarRecargaTesoreria(user.getId(), request);
        return ResponseEntity
                .ok(DataResponseDTO.success(dto, "Recarga por tesorería generada. Lleve el ticket a ventanilla."));
    }

    @GetMapping("/recargas/tesoreria/pendientes")
    @PreAuthorize("hasRole('TESORERIA')")
    @Operation(summary = "Listar recargas por tesorería pendientes de confirmación")
    public ResponseEntity<DataResponseDTO<List<RecargaMonederoDTO>>> listarRecargasTesoreriaPendientes() {
        List<RecargaMonederoDTO> lista = monederoService.listarRecargasTesoreriaPendientes();
        return ResponseEntity.ok(DataResponseDTO.success(lista, "Recargas pendientes en ventanilla"));
    }

    @GetMapping("/recargas/tesoreria/confirmadas")
    @PreAuthorize("hasRole('TESORERIA')")
    @Operation(summary = "Listar recargas por tesorería ya confirmadas")
    public ResponseEntity<DataResponseDTO<List<RecargaMonederoDTO>>> listarRecargasTesoreriaConfirmadas() {
        List<RecargaMonederoDTO> lista = monederoService.listarRecargasTesoreriaConfirmadas();
        return ResponseEntity.ok(DataResponseDTO.success(lista, "Recargas confirmadas por tesorería"));
    }

    @PostMapping("/recargas/tesoreria/{recargaId}/confirmar")
    @PreAuthorize("hasRole('TESORERIA')")
    @Operation(summary = "Confirmar pago de recarga por tesorería y acreditar el saldo")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> confirmarRecargaTesoreria(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId) {
        RecargaMonederoDTO dto = monederoService.confirmarRecargaTesoreria(recargaId, user.getId());
        return ResponseEntity
                .ok(DataResponseDTO.success(dto, "Pago confirmado por tesorería y saldo acreditado."));
    }

    @PostMapping(value = "/recargas/tesoreria/{recargaId}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Adjuntar o reemplazar el recibo de tesorería y enviarlo a Finanzas")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> adjuntarComprobanteTesoreria(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId,
            @RequestPart("comprobante") MultipartFile comprobante) {
        RecargaMonederoDTO dto = monederoService.adjuntarComprobanteTesoreria(user.getId(), recargaId, comprobante);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Comprobante recibido y enviado a Finanzas para revisión."));
    }

    @PostMapping("/recargas/tesoreria/{recargaId}/enviar-finanzas")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Enviar a Finanzas una recarga de tesorería con recibo adjunto")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> enviarRecargaTesoreriaAFinanzas(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId) {
        RecargaMonederoDTO dto = monederoService.enviarRecargaTesoreriaAFinanzas(user.getId(), recargaId);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Recarga enviada a Finanzas para revisión."));
    }

    @PostMapping("/recargas/tesoreria/{recargaId}/cancelar")
    @PreAuthorize("hasRole('ALUMNO')")
    @Operation(summary = "Cancelar una solicitud de recarga de tesorería aún pendiente")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> cancelarRecargaTesoreria(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId) {
        RecargaMonederoDTO dto = monederoService.cancelarRecargaTesoreria(user.getId(), recargaId);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Solicitud de recarga cancelada."));
    }

    @GetMapping("/recargas/{recargaId}/comprobante")
    @PreAuthorize("hasAnyRole('ALUMNO','TESORERIA','ADMIN')")
    @Operation(summary = "Descargar el comprobante de una recarga autorizada")
    public ResponseEntity<Resource> obtenerComprobanteRecarga(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId) {
        Resource comprobante = monederoService.obtenerComprobanteRecarga(user.getId(), recargaId);
        MediaType mediaType = MediaTypeFactory.getMediaType(comprobante)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + comprobante.getFilename() + "\"")
                .body(comprobante);
    }

    @GetMapping("/recargas/tesoreria/{recargaId}/ticket")
    @PreAuthorize("hasAnyRole('ALUMNO','TESORERIA','ADMIN')")
    @Operation(summary = "Obtener ticket/comprobante de recarga por tesorería")
    public ResponseEntity<DataResponseDTO<RecargaMonederoDTO>> obtenerTicketRecargaTesoreria(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long recargaId) {
        RecargaMonederoDTO dto = monederoService.obtenerTicketRecargaTesoreria(user.getId(), recargaId);
        return ResponseEntity.ok(DataResponseDTO.success(dto, "Ticket de recarga por tesorería"));
    }
}
