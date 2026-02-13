package com.api_caich.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.entity.Producto;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.InventarioService;
import com.api_caich.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "APIs for inventory management")
@SecurityRequirement(name = "bearerAuth")
public class InventarioController {

    private final InventarioService inventarioService;
    private final ProductoRepository productoRepository;
    private final ProductoService productoService; // Inyectamos el servicio de producto

    // --- ENDPOINTS DE GESTIÓN (CRUD) ---
    @PostMapping("/productos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto", description = "Registra un nuevo producto en el catálogo")
    public ResponseEntity<DataResponseDTO<Producto>> crearProducto(@RequestBody Producto producto) {
        Producto nuevo = productoService.crearProducto(producto);
        return ResponseEntity.ok(DataResponseDTO.success(nuevo, "Producto creado exitosamente"));
    }

    @PutMapping("/productos/{productoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Actualiza los datos básicos de un producto")
    public ResponseEntity<DataResponseDTO<Producto>> actualizarProducto(
            @PathVariable Long productoId,
            @RequestBody Producto producto) {
        Producto actualizado = productoService.actualizarProducto(productoId, producto);
        return ResponseEntity.ok(DataResponseDTO.success(actualizado, "Producto actualizado exitosamente"));
    }

    @DeleteMapping("/productos/{productoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar producto", description = "Realiza un borrado lógico del producto")
    public ResponseEntity<BaseResponseDTO> desactivarProducto(@PathVariable Long productoId) {
        productoService.desactivarProducto(productoId);
        return ResponseEntity.ok(BaseResponseDTO.success("Producto desactivado correctamente"));
    }

    @GetMapping("/productos/{productoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Obtener producto por ID")
    public ResponseEntity<DataResponseDTO<Producto>> obtenerPorId(@PathVariable Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return ResponseEntity.ok(DataResponseDTO.success(producto, "Producto encontrado"));
    }

    @PostMapping("/productos/{productoId}/reducir-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Reduce product stock", description = "Reduce stock and create inventory movement")
    public ResponseEntity<BaseResponseDTO> reducirStock(
            @PathVariable Long productoId,
            @RequestParam Integer cantidad,
            @RequestParam String motivo,
            @RequestParam String referencia,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
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
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
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
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
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

    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get all products", description = "Get paginated list of products")
    public ResponseEntity<DataResponseDTO<Page<Producto>>> listarProductos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean estaActivo,
            @RequestParam(required = false) Boolean esPrestable,
            @RequestParam(required = false) Boolean esVendible,
            Pageable pageable) {

        Page<Producto> productos = productoService.listarProductosPaginados(
                nombre, categoria, estaActivo, esPrestable, esVendible, pageable);

        return ResponseEntity.ok(DataResponseDTO.success(productos, "Inventario obtenido"));
    }
}
