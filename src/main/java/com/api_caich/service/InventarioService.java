package com.api_caich.service;

import com.api_caich.entity.MovimientoInventario;
import com.api_caich.entity.Producto;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.MovimientoInventarioRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public void reducirStock(Long productoId, Integer cantidad, String motivo, String referencia, Long usuarioId) {
        log.info("Reducing stock for product: {}, quantity: {}", productoId, cantidad);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto not found"));

        if (producto.getStock() < cantidad) {
            throw new BusinessException("Stock insuficiente para: " + producto.getNombre() +
                    ". Disponible: " + producto.getStock() +
                    ", Solicitado: " + cantidad);
        }

        Integer stockAnterior = producto.getStock();
        Integer stockNuevo = stockAnterior - cantidad;

        producto.setStock(stockNuevo);
        productoRepository.save(producto);

        // Registrar movimiento
        registrarMovimiento(producto, "Salida", cantidad, stockAnterior, stockNuevo,
                motivo, referencia, usuarioId);

        // Verificar stock bajo
        if (stockNuevo <= producto.getStockMinimo()) {
            notificacionService.notificarStockBajo(productoId, producto.getNombre(), stockNuevo);
        }

        log.info("Stock reduced successfully for product: {}", producto.getNombre());
    }

    @Transactional
    public void incrementarStock(Long productoId, Integer cantidad, String motivo, String referencia, Long usuarioId) {
        log.info("Increasing stock for product: {}, quantity: {}", productoId, cantidad);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto not found"));

        Integer stockAnterior = producto.getStock();
        Integer stockNuevo = stockAnterior + cantidad;

        producto.setStock(stockNuevo);
        productoRepository.save(producto);

        // Registrar movimiento
        registrarMovimiento(producto, "Devolucion", cantidad, stockAnterior, stockNuevo,
                motivo, referencia, usuarioId);

        log.info("Stock increased successfully for product: {}", producto.getNombre());
    }

    @Transactional
    public void ajustarStock(Long productoId, Integer nuevoStock, String motivo, Long usuarioId) {
        log.info("Adjusting stock for product: {}, new stock: {}", productoId, nuevoStock);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto not found"));

        if (nuevoStock < 0) {
            throw new BusinessException("Stock cannot be negative");
        }

        Integer stockAnterior = producto.getStock();
        Integer diferencia = nuevoStock - stockAnterior;

        producto.setStock(nuevoStock);
        productoRepository.save(producto);

        // Registrar movimiento
        registrarMovimiento(producto, "Ajuste", Math.abs(diferencia), stockAnterior, nuevoStock,
                motivo, "AJUSTE_MANUAL", usuarioId);

        log.info("Stock adjusted successfully for product: {}", producto.getNombre());
    }

    private void registrarMovimiento(Producto producto, String tipoMovimiento, Integer cantidad,
            Integer stockAnterior, Integer stockNuevo, String motivo,
            String referencia, Long usuarioId) {

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setMotivo(motivo);
        movimiento.setReferencia(referencia);

        if (usuarioId != null) {
            movimiento.setRealizadoPor(usuarioRepository.findById(usuarioId).orElse(null));
        }

        movimientoRepository.save(movimiento);
    }

    public List<Producto> obtenerProductosStockBajo() {
        return productoRepository.findProductosStockBajo();
    }

    public List<Producto> obtenerProductosSinStock() {
        return productoRepository.findProductosSinStock();
    }
}