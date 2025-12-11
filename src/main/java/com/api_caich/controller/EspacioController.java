package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.espacio.ActualizarEspacioDTO;
import com.api_caich.dto.espacio.CrearEspacioDTO;
import com.api_caich.dto.espacio.EspacioDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.EspacioService;
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
@RequestMapping("/espacios")
@RequiredArgsConstructor
@Tag(name = "Spaces", description = "APIs for managing spaces")
@SecurityRequirement(name = "bearerAuth")
public class EspacioController {

    private final EspacioService espacioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get all spaces", description = "Get list of all spaces")
    public ResponseEntity<DataResponseDTO<List<EspacioDTO>>> obtenerTodosEspacios() {
        List<EspacioDTO> espacios = espacioService.obtenerTodosEspacios();
        return ResponseEntity.ok(DataResponseDTO.success(espacios, "Spaces retrieved successfully"));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get active spaces", description = "Get list of active spaces")
    public ResponseEntity<DataResponseDTO<List<EspacioDTO>>> obtenerEspaciosActivos() {
        List<EspacioDTO> espacios = espacioService.obtenerEspaciosActivos();
        return ResponseEntity.ok(DataResponseDTO.success(espacios, "Active spaces retrieved successfully"));
    }

    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get spaces by type", description = "Get list of spaces by specific type")
    public ResponseEntity<DataResponseDTO<List<EspacioDTO>>> obtenerEspaciosPorTipo(
            @Parameter(description = "Space type") @PathVariable String tipo) {

        List<EspacioDTO> espacios = espacioService.obtenerEspaciosPorTipo(tipo);
        return ResponseEntity.ok(DataResponseDTO.success(espacios, "Spaces by type retrieved successfully"));
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get available spaces by capacity", description = "Get list of available spaces with minimum capacity")
    public ResponseEntity<DataResponseDTO<List<EspacioDTO>>> obtenerEspaciosDisponiblesPorCapacidad(
            @Parameter(description = "Minimum capacity") @RequestParam Integer capacidadMinima) {

        List<EspacioDTO> espacios = espacioService.obtenerEspaciosDisponiblesPorCapacidad(capacidadMinima);
        return ResponseEntity.ok(DataResponseDTO.success(espacios, "Available spaces retrieved successfully"));
    }

    @GetMapping("/{espacioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get space by ID", description = "Get space details by ID")
    public ResponseEntity<DataResponseDTO<EspacioDTO>> obtenerEspacioPorId(
            @Parameter(description = "Space ID") @PathVariable Long espacioId) {

        EspacioDTO espacio = espacioService.obtenerEspacioPorId(espacioId);
        return ResponseEntity.ok(DataResponseDTO.success(espacio, "Space retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create space", description = "Create a new space")
    public ResponseEntity<DataResponseDTO<EspacioDTO>> crearEspacio(
            @Valid @RequestBody CrearEspacioDTO crearEspacioDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        EspacioDTO espacio = espacioService.crearEspacio(crearEspacioDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(espacio, "Space created successfully"));
    }

    @PutMapping("/{espacioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update space", description = "Update space details")
    public ResponseEntity<DataResponseDTO<EspacioDTO>> actualizarEspacio(
            @Parameter(description = "Space ID") @PathVariable Long espacioId,
            @Valid @RequestBody ActualizarEspacioDTO actualizarEspacioDTO,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        EspacioDTO espacio = espacioService.actualizarEspacio(espacioId, actualizarEspacioDTO, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(espacio, "Space updated successfully"));
    }

    @PutMapping("/{espacioId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change space status", description = "Change space active status")
    public ResponseEntity<DataResponseDTO<EspacioDTO>> cambiarEstadoEspacio(
            @Parameter(description = "Space ID") @PathVariable Long espacioId,
            @RequestParam Boolean estaActivo,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        EspacioDTO espacio = espacioService.cambiarEstadoEspacio(espacioId, estaActivo, usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(espacio, "Space status updated successfully"));
    }

    @DeleteMapping("/{espacioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete space", description = "Delete space (only if no related tickets exist)")
    public ResponseEntity<BaseResponseDTO> eliminarEspacio(
            @Parameter(description = "Space ID") @PathVariable Long espacioId,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        espacioService.eliminarEspacio(espacioId, usuarioId);
        return ResponseEntity.ok(BaseResponseDTO.success("Space deleted successfully"));
    }

    @GetMapping("/stats/activos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active spaces count", description = "Get count of active spaces")
    public ResponseEntity<DataResponseDTO<Long>> contarEspaciosActivos() {
        Long count = espacioService.contarEspaciosActivos();
        return ResponseEntity.ok(DataResponseDTO.success(count, "Active spaces count retrieved"));
    }

    @GetMapping("/tipos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA', 'ALUMNO')")
    @Operation(summary = "Get space types", description = "Get list of available space types")
    public ResponseEntity<DataResponseDTO<List<String>>> obtenerTiposEspacio() {
        List<String> tipos = espacioService.obtenerTiposEspacio();
        return ResponseEntity.ok(DataResponseDTO.success(tipos, "Space types retrieved successfully"));
    }
}
