package com.api_caich.controller;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.entity.Producto;
import com.api_caich.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "APIs for inventory management")
@SecurityRequirement(name = "bearerAuth")
public class InventarioController {

    private final InventarioService inventarioService;

    @PostMapping("/productos/{productoId}/reducir-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Reduce product stock", description = "Reduce stock and create inventory movement")
    public ResponseEntity<BaseResponseDTO> reducirStock(
            @PathVariable Long productoId,
            @RequestParam Integer cantidad,
            @RequestParam String motivo,
            @RequestParam String referencia,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        inventarioService.reducirStock(productoId, cantidad, motivo, referencia, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Stock reduced successfully"));
    }

    @PostMapping("/productos/{productoId}/incrementar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Increase product stock", description = "Increase stock and create inventory movement")
    public ResponseEntity<BaseResponseDTO> incrementarStock(
            @PathVariable Long productoId,
            @RequestParam Integer cantidad,
            @RequestParam String motivo,
            @RequestParam String referencia,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        inventarioService.incrementarStock(productoId, cantidad, motivo, referencia, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Stock increased successfully"));
    }

    @PutMapping("/productos/{productoId}/ajustar-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust product stock", description = "Manual stock adjustment for physical inventory")
    public ResponseEntity<BaseResponseDTO> ajustarStock(
            @PathVariable Long productoId,
            @RequestParam Integer nuevoStock,
            @RequestParam String motivo,
            Authentication authentication) {

        Long usuarioId = Long.parseLong(authentication.getName());
        inventarioService.ajustarStock(productoId, nuevoStock, motivo, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Stock adjusted successfully"));
    }

    @GetMapping("/productos/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get low stock products", description = "Get products with stock below minimum threshold")
    public ResponseEntity<DataResponseDTO<List<Producto>>> obtenerProductosStockBajo() {
        List<Producto> productos = inventarioService.obtenerProductosStockBajo();
        return ResponseEntity.ok(DataResponseDTO.success(productos, "Low stock products retrieved"));
    }

    @GetMapping("/productos/sin-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get out of stock products", description = "Get products with zero stock")
    public ResponseEntity<DataResponseDTO<List<Producto>>> obtenerProductosSinStock() {
        List<Producto> productos = inventarioService.obtenerProductosSinStock();
        return ResponseEntity.ok(DataResponseDTO.success(productos, "Out of stock products retrieved"));
    }
}