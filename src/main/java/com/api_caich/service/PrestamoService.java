package com.api_caich.service;

import com.api_caich.dto.prestamo.CrearPrestamoDTO;
import com.api_caich.dto.prestamo.PrestamoDTO;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Ticket;
import com.api_caich.entity.TicketPrestamo;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoPrestamo;
import com.api_caich.enums.EstadoTicket;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.TicketPrestamoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final TicketPrestamoRepository ticketPrestamoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PrestamoDTO crearPrestamo(CrearPrestamoDTO prestamoDTO, Long usuarioId) {
        log.info("Creating loan for user: {}, product: {}", prestamoDTO.getAlumnoId(), prestamoDTO.getProductoId());

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Producto producto = productoRepository.findById(prestamoDTO.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
            throw new BusinessException("El producto no es prestable: " + producto.getNombre());
        }

        if (producto.getStock() <= 0) {
            throw new BusinessException("Stock insuficiente para préstamo: " + producto.getNombre());
        }

        // Create ticket for loan
        Ticket ticket = new Ticket();
        ticket.setCreadoPor(usuario);
        ticket.setEstadoTicket(EstadoTicket.BORRADOR);
        ticket.setMontoTotal(producto.getPrecio());
        ticket = ticketRepository.save(ticket);

        // Create TicketPrestamo
        TicketPrestamo prestamo = new TicketPrestamo();
        prestamo.setTicket(ticket);
        prestamo.setProducto(producto);
        prestamo.setResponsable(usuario);
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setFechaDevolucionEstimada(prestamoDTO.getFechaDevolucionEstimada());
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        prestamo = ticketPrestamoRepository.save(prestamo);

        // Reduce product stock
        producto.setStock(producto.getStock() - 1);
        productoRepository.save(producto);

        // Log audit
        auditoriaService.registrarCambio("Prestamos", prestamo.getId(), "Crear",
                "Nuevo préstamo creado", null, prestamo.toString(), usuarioId, usuario.getEmail());

        log.info("Prestamo creado: {} para producto: {}", prestamo.getId(), producto.getNombre());

        return mapToDTO(prestamo);
    }

    @Transactional
    public PrestamoDTO crearPrestamoAlumno(CrearPrestamoDTO prestamoDTO, Long usuarioId) {
        // Alias for crearPrestamo for backward compatibility
        return crearPrestamo(prestamoDTO, usuarioId);
    }

    @Transactional
    public PrestamoDTO devolverPrestamo(Long prestamoId, String observaciones, Long usuarioId) {
        log.info("Devolviendo préstamo: {}", prestamoId);

        TicketPrestamo prestamo = ticketPrestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo not found"));

        if (!EstadoPrestamo.ACTIVO.equals(prestamo.getEstado())) {
            throw new BusinessException("El préstamo no está activo");
        }

        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);

        prestamo = ticketPrestamoRepository.save(prestamo);

        // Return product to stock
        Producto producto = prestamo.getProducto();
        producto.setStock(producto.getStock() + 1);
        productoRepository.save(producto);

        // Update ticket status
        Ticket ticket = prestamo.getTicket();
        ticket.setEstadoTicket(EstadoTicket.COMPLETADO);
        ticketRepository.save(ticket);

        // Log audit
        auditoriaService.registrarCambio("Prestamos", prestamoId, "Devolver",
                "Préstamo devuelto: " + observaciones, prestamo.toString(), null, usuarioId, null);

        try {
            log.info("Devolución de préstamo notificada a: {}", prestamo.getResponsable().getEmail());
        } catch (Exception e) {
            log.warn("Error al notificar devolución", e);
        }

        return mapToDTO(prestamo);
    }

    @Transactional
    public PrestamoDTO devolverPrestamoAlumno(Long prestamoId, Long usuarioId, String observaciones) {
        // Alias for devolverPrestamo for backward compatibility
        return devolverPrestamo(prestamoId, observaciones, usuarioId);
    }

    @Transactional
    public void marcarComoPerdido(Long prestamoId, String observaciones, Long usuarioId) {
        log.info("Marcando préstamo como perdido: {}", prestamoId);

        TicketPrestamo prestamo = ticketPrestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo not found"));

        prestamo.setEstado(EstadoPrestamo.PERDIDO);

        ticketPrestamoRepository.save(prestamo);

        // Mark ticket as cancelled
        Ticket ticket = prestamo.getTicket();
        ticket.setEstadoTicket(EstadoTicket.CANCELADO);
        ticketRepository.save(ticket);

        // Log audit
        auditoriaService.registrarCambio("Prestamos", prestamoId, "Marcar como perdido",
                "Préstamo marcado como perdido: " + observaciones, null, null, usuarioId, null);

        try {
            log.info("Pérdida de préstamo notificada a: {}", prestamo.getResponsable().getEmail());
        } catch (Exception e) {
            log.warn("Error al notificar pérdida", e);
        }
    }

    public List<PrestamoDTO> obtenerPrestamosActivosPorAlumno(Long alumnoId) {
        Usuario usuario = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario not found"));
        
        List<TicketPrestamo> prestamos = ticketPrestamoRepository.findAll().stream()
                .filter(p -> p.getResponsable().equals(usuario) && EstadoPrestamo.ACTIVO.equals(p.getEstado()))
                .collect(Collectors.toList());
        return prestamos.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<PrestamoDTO> obtenerPrestamosPorEstado(String estado) {
        List<TicketPrestamo> prestamos = ticketPrestamoRepository.findByEstado(estado);
        return prestamos.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public Page<PrestamoDTO> obtenerMisPrestamos(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario not found"));
        
        List<TicketPrestamo> todoPrestamos = ticketPrestamoRepository.findAll();
        List<TicketPrestamo> misPrestamos = todoPrestamos.stream()
                .filter(p -> p.getResponsable().equals(usuario))
                .collect(Collectors.toList());
        
        org.springframework.data.domain.PageImpl<TicketPrestamo> page = 
                new org.springframework.data.domain.PageImpl<>(misPrestamos, pageable, misPrestamos.size());
        return page.map(this::mapToDTO);
    }

    @Transactional
    public void eliminarPrestamo(Long prestamoId, Long usuarioId) {
        log.info("Eliminando préstamo: {} por usuario: {}", prestamoId, usuarioId);

        TicketPrestamo prestamo = ticketPrestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo not found"));

        ticketPrestamoRepository.delete(prestamo);

        // Log audit
        auditoriaService.registrarCambio("Prestamos", prestamoId, "Eliminar",
                "Préstamo eliminado", prestamo.toString(), null, usuarioId, null);
    }

    private PrestamoDTO mapToDTO(TicketPrestamo prestamo) {
        return PrestamoDTO.builder()
                .prestamoId(prestamo.getId())
                .alumnoNombre(prestamo.getResponsable().getNombreCompleto())
                .productoNombre(prestamo.getProducto().getNombre())
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaDevolucionEstimada(prestamo.getFechaDevolucionEstimada())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .estadoPrestamo(prestamo.getEstado() != null ? prestamo.getEstado().getDescripcion() : null)
                .build();
    }
}
