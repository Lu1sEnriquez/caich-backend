package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.cuenta.ActualizarCuentaBancariaDTO;
import com.api_caich.dto.cuenta.CrearCuentaBancariaDTO;
import com.api_caich.dto.cuenta.CuentaBancariaDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.CuentaBancariaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas-bancarias")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "APIs for managing bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class CuentaBancariaController {

    private final CuentaBancariaService cuentaBancariaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bank accounts", description = "Get list of all bank accounts")
    public ResponseEntity<DataResponseDTO<List<CuentaBancariaDTO>>> obtenerTodasCuentas() {
        List<CuentaBancariaDTO> cuentas = cuentaBancariaService.obtenerTodasCuentas();
        return ResponseEntity.ok(DataResponseDTO.success(cuentas, "Bank accounts retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search bank accounts", description = "Search bank accounts by bank, account number, holder or CLABE")
    public ResponseEntity<DataResponseDTO<List<CuentaBancariaDTO>>> buscarCuentas(
            @Parameter(description = "Search term") @RequestParam String search) {

        List<CuentaBancariaDTO> cuentas = cuentaBancariaService.buscarCuentas(search);
        return ResponseEntity.ok(DataResponseDTO.success(cuentas, "Search results retrieved"));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get active bank accounts", description = "Get list of active bank accounts")
    public ResponseEntity<DataResponseDTO<List<CuentaBancariaDTO>>> obtenerCuentasActivas() {
        List<CuentaBancariaDTO> cuentas = cuentaBancariaService.obtenerCuentasActivas();
        return ResponseEntity.ok(DataResponseDTO.success(cuentas, "Active bank accounts retrieved"));
    }

    @GetMapping("/estado/{activa}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get bank accounts by status", description = "Get list of bank accounts by active status")
    public ResponseEntity<DataResponseDTO<List<CuentaBancariaDTO>>> obtenerCuentasPorEstado(
            @Parameter(description = "Active status") @PathVariable Boolean activa) {

        List<CuentaBancariaDTO> cuentas = cuentaBancariaService.obtenerCuentasPorEstado(activa);
        return ResponseEntity.ok(DataResponseDTO.success(cuentas, "Bank accounts by status retrieved"));
    }

    @GetMapping("/banco/{banco}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get bank accounts by bank", description = "Get list of bank accounts by bank name")
    public ResponseEntity<DataResponseDTO<List<CuentaBancariaDTO>>> obtenerCuentasPorBanco(
            @Parameter(description = "Bank name") @PathVariable String banco) {

        List<CuentaBancariaDTO> cuentas = cuentaBancariaService.obtenerCuentasPorBanco(banco);
        return ResponseEntity.ok(DataResponseDTO.success(cuentas, "Bank accounts by bank retrieved"));
    }

    @GetMapping("/{cuentaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get bank account by ID", description = "Get bank account details by ID")
    public ResponseEntity<DataResponseDTO<CuentaBancariaDTO>> obtenerCuentaPorId(
            @Parameter(description = "Bank account ID") @PathVariable Long cuentaId) {

        CuentaBancariaDTO cuenta = cuentaBancariaService.obtenerCuentaPorId(cuentaId);
        return ResponseEntity.ok(DataResponseDTO.success(cuenta, "Bank account retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create bank account", description = "Create a new bank account")
    public ResponseEntity<DataResponseDTO<CuentaBancariaDTO>> crearCuenta(
            @Valid @RequestBody CrearCuentaBancariaDTO crearCuentaDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        CuentaBancariaDTO cuenta = cuentaBancariaService.crearCuenta(crearCuentaDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(cuenta, "Bank account created successfully"));
    }

    @PutMapping("/{cuentaId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update bank account", description = "Update bank account details")
    public ResponseEntity<DataResponseDTO<CuentaBancariaDTO>> actualizarCuenta(
            @Parameter(description = "Bank account ID") @PathVariable Long cuentaId,
            @Valid @RequestBody ActualizarCuentaBancariaDTO actualizarCuentaDTO,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        CuentaBancariaDTO cuenta = cuentaBancariaService.actualizarCuenta(cuentaId, actualizarCuentaDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(cuenta, "Bank account updated successfully"));
    }

    @PutMapping("/{cuentaId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change bank account status", description = "Change bank account active status")
    public ResponseEntity<DataResponseDTO<CuentaBancariaDTO>> cambiarEstadoCuenta(
            @Parameter(description = "Bank account ID") @PathVariable Long cuentaId,
            @RequestParam Boolean estaActiva,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        CuentaBancariaDTO cuenta = cuentaBancariaService.cambiarEstadoCuenta(cuentaId, estaActiva, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(cuenta, "Bank account status updated successfully"));
    }

    @DeleteMapping("/{cuentaId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete bank account", description = "Delete bank account (only if no related tickets exist)")
    public ResponseEntity<BaseResponseDTO> eliminarCuenta(
            @Parameter(description = "Bank account ID") @PathVariable Long cuentaId,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        cuentaBancariaService.eliminarCuenta(cuentaId, usuarioId);
        return ResponseEntity.ok(BaseResponseDTO.success("Bank account deleted successfully"));
    }

    @GetMapping("/stats/activas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active bank accounts count", description = "Get count of active bank accounts")
    public ResponseEntity<DataResponseDTO<Long>> contarCuentasActivas() {
        Long count = cuentaBancariaService.contarCuentasActivas();
        return ResponseEntity.ok(DataResponseDTO.success(count, "Active bank accounts count retrieved"));
    }
}