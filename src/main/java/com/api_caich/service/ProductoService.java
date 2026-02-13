package com.api_caich.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api_caich.entity.MovimientoInventario;
import com.api_caich.entity.Producto;
import com.api_caich.repository.MovimientoInventarioRepository;
import com.api_caich.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repository;
    private final MovimientoInventarioRepository movimientoRepo;

    @Transactional
    public Producto crearProducto(Producto producto) {
        log.info("Iniciando validación y registro de producto: {}", producto.getNombre());

        // 1. Validaciones de Negocio Obligatorias
        if (producto.getCodigo() == null || producto.getCodigo().trim().isEmpty()) {
            throw new RuntimeException("El código del producto es obligatorio.");
        }

        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            throw new RuntimeException("El nombre del producto es obligatorio.");
        }

        // 2. Verificar duplicados (Importante para evitar excepciones de SQL directas)
        if (repository.findByCodigo(producto.getCodigo()).isPresent()) {
            throw new RuntimeException("Ya existe un producto registrado con el código: " + producto.getCodigo());
        }

        // 3. Seteo de Valores por Defecto (Protección contra Nulos del JSON)
        if (producto.getPrecio() == null) {
            producto.setPrecio(BigDecimal.ZERO);
        }

        if (producto.getStock() == null) {
            producto.setStock(0);
        }

        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(5); // Valor por defecto del negocio
        }

        if (producto.getEsVendible() == null) {
            producto.setEsVendible(true);
        }

        if (producto.getEsPrestable() == null) {
            producto.setEsPrestable(false);
        }

        // 4. Forzar estado inicial
        producto.setEstaActivo(true);

        // 5. Guardado Final
        try {
            return repository.save(producto);
        } catch (Exception e) {
            log.error("Error al persistir el producto: {}", e.getMessage());
            throw new RuntimeException("No se pudo guardar el producto debido a un error de integridad.");
        }
    }

    @Transactional
    public Producto actualizarProducto(Long productoId, Producto productoActualizado) {
        log.info("Actualizando producto: {}", productoId);

        Producto producto = repository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setNombre(productoActualizado.getNombre());
        producto.setCodigo(productoActualizado.getCodigo());
        producto.setCategoria(productoActualizado.getCategoria());
        producto.setPrecio(productoActualizado.getPrecio()); // Asegúrate de incluir el precio
        producto.setStockMinimo(productoActualizado.getStockMinimo());
        producto.setEstaActivo(productoActualizado.getEstaActivo());
        producto.setEsPrestable(productoActualizado.getEsPrestable());
        producto.setEsVendible(productoActualizado.getEsVendible());

        return repository.save(producto);
    }

    @Transactional
    public void desactivarProducto(Long productoId) {
        log.info("Desactivando producto: {}", productoId);

        Producto producto = repository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setEstaActivo(false);
        repository.save(producto);
    }

    // Ejemplo de lógica necesaria en el ProductoService
    @Transactional
    public void registrarSalida(Long productoId, Integer cantidad, String motivo, String referencia) {
        Producto producto = repository.findById(productoId).orElseThrow();

        // 1. Guardar Historial
        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(producto);
        mov.setStockAnterior(producto.getStock());
        mov.setCantidad(cantidad);
        mov.setStockNuevo(producto.getStock() - cantidad);
        mov.setTipoMovimiento("SALIDA");
        mov.setMotivo(motivo);
        mov.setReferencia(referencia);
        movimientoRepo.save(mov);

        // 2. Actualizar Stock Real
        producto.setStock(producto.getStock() - cantidad);
        repository.save(producto);
    }

    /**
     * Obtiene productos paginados, permitiendo filtrar por nombre
     * opcionalmente.
     */
    @Transactional(readOnly = true)
    public Page<Producto> listarProductosPaginados(
            String nombre,
            String categoria,
            Boolean activo,
            Boolean prestable,
            Boolean vendible,
            Pageable pageable) {

        log.info("Consultando inventario con filtros avanzados");

        // Normalizamos el string de nombre para la búsqueda
        String nombreFiltro = (nombre != null && !nombre.trim().isEmpty()) ? nombre : null;
        String catFiltro = (categoria != null && !categoria.trim().isEmpty()) ? categoria : null;

        return repository.findAllConFiltros(
                nombreFiltro,
                catFiltro,
                activo,
                prestable,
                vendible,
                pageable);
    }
}
