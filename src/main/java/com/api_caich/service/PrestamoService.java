package com.api_caich.service;

import com.api_caich.dto.prestamo.CrearPrestamoDTO;
import com.api_caich.dto.prestamo.PrestamoDTO;
import com.api_caich.entity.PrestamoMaterial;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Usuario;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.PrestamoMaterialRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.TicketRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoMaterialRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final TicketRepository ticketRepository;
    private final InventarioService inventarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PrestamoDTO crearPrestamo(CrearPrestamoDTO prestamoDTO, Long usuarioId) {
        log.info("Creating loan for alumno: {}, producto: {}", prestamoDTO.getAlumnoId(), prestamoDTO.getProductoId());

        // Validar que el producto sea prestable
        Producto producto = productoRepository.findById(prestamoDTO.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto not found"));

        if (!Boolean.TRUE.equals(producto.getEsPrestable())) {
            throw new BusinessException("El producto no es prestable: " + producto.getNombre());
        }

        // Validar stock disponible
        if (producto.getStock() < prestamoDTO.getCantidad()) {
            throw new BusinessException("Stock insuficiente para préstamo: " + producto.getNombre());
        }

        // Obtener entidades relacionadas
        Usuario alumno = usuarioRepository.findById(prestamoDTO.getAlumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno not found"));

        // Crear préstamo
        PrestamoMaterial prestamo = new PrestamoMaterial();
        prestamo.setAlumno(alumno);
        prestamo.setProducto(producto);
        prestamo.setFechaPrestamo(prestamoDTO.getFechaPrestamo());
        prestamo.setFechaDevolucionEstimada(prestamoDTO.getFechaDevolucionEstimada());
        prestamo.setCantidad(prestamoDTO.getCantidad());
        prestamo.setObservaciones(prestamoDTO.getObservaciones());

        // Asociar ticket si existe
        if (prestamoDTO.getTicketId() != null) {
            prestamo.setTicket(ticketRepository.findById(prestamoDTO.getTicketId()).orElse(null));
        }

        // Verificar si ya está vencido
        if (prestamo.getFechaDevolucionEstimada().isBefore(LocalDateTime.now())) {
            prestamo.setEstadoPrestamo("Vencido");
        }

        PrestamoMaterial savedPrestamo = prestamoRepository.save(prestamo);

        // Reducir stock
        inventarioService.reducirStock(producto.getProductoId(), prestamoDTO.getCantidad(),
                "Préstamo: " + savedPrestamo.getPrestamoId(),
                "PRESTAMO_" + savedPrestamo.getPrestamoId(), usuarioId);

        // Auditoría
        auditoriaService.registrarCambio("PrestamosMateriales", savedPrestamo.getPrestamoId(), "Crear",
                "Todos los campos", null, null, usuarioId, "SYSTEM");

        log.info("Loan created successfully with ID: {}", savedPrestamo.getPrestamoId());
        return mapToDTO(savedPrestamo);
    }

    @Transactional
    public PrestamoDTO devolverPrestamo(Long prestamoId, String observaciones, Long usuarioId) {
        PrestamoMaterial prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo not found"));

        if (!List.of("Activo", "Vencido").contains(prestamo.getEstadoPrestamo())) {
            throw new BusinessException("Solo se pueden devolver préstamos en estado Activo o Vencido");
        }

        prestamo.setEstadoPrestamo("Devuelto");
        prestamo.setFechaDevolucionReal(LocalDateTime.now());

        if (observaciones != null) {
            String nuevasObservaciones = prestamo.getObservaciones() != null
                    ? prestamo.getObservaciones() + "\nDevolución: " + observaciones
                    : observaciones;
            prestamo.setObservaciones(nuevasObservaciones);
        }

        PrestamoMaterial updatedPrestamo = prestamoRepository.save(prestamo);

        // Devolver stock
        inventarioService.incrementarStock(prestamo.getProducto().getProductoId(),
                prestamo.getCantidad(),
                "Devolución de préstamo: " + prestamoId,
                "PRESTAMO_" + prestamoId, usuarioId);

        // Auditoría
        auditoriaService.registrarCambio("PrestamosMateriales", prestamoId, "Actualizar",
                "EstadoPrestamo, FechaDevolucionReal", null, null, usuarioId, "SYSTEM");

        return mapToDTO(updatedPrestamo);
    }

    @Transactional
    public void marcarComoPerdido(Long prestamoId, String observaciones, Long usuarioId) {
        PrestamoMaterial prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo not found"));

        if (observaciones == null || observaciones.trim().isEmpty()) {
            throw new BusinessException("Las observaciones son obligatorias para marcar como perdido");
        }

        prestamo.setEstadoPrestamo("Perdido");
        prestamo.setObservaciones(observaciones);
        prestamoRepository.save(prestamo);

        // Crear notificación para administradores
        notificacionService.notificarMaterialPerdido(prestamoId, prestamo.getProducto().getNombre(),
                prestamo.getAlumno().getNombreCompleto());

        // Auditoría
        auditoriaService.registrarCambio("PrestamosMateriales", prestamoId, "Actualizar",
                "EstadoPrestamo a Perdido", null, null, usuarioId, "SYSTEM");
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void actualizarPrestamosVencidos() {
        log.info("Checking for overdue loans...");

        List<PrestamoMaterial> prestamosVencidos = prestamoRepository
                .findByEstadoPrestamoAndFechaDevolucionEstimadaBefore("Activo", LocalDateTime.now());

        for (PrestamoMaterial prestamo : prestamosVencidos) {
            prestamo.setEstadoPrestamo("Vencido");
            prestamoRepository.save(prestamo);

            // Crear notificación
            long diasVencidos = java.time.temporal.ChronoUnit.DAYS.between(
                    prestamo.getFechaDevolucionEstimada(), LocalDateTime.now());

            notificacionService.notificarMaterialVencido(
                    prestamo.getAlumno().getUsuarioId(),
                    prestamo.getPrestamoId(),
                    prestamo.getProducto().getNombre(),
                    (int) diasVencidos);

            log.info("Updated overdue loan: {} for product: {}", prestamo.getPrestamoId(),
                    prestamo.getProducto().getNombre());
        }

        log.info("Updated {} overdue loans", prestamosVencidos.size());
    }

    public List<PrestamoDTO> obtenerPrestamosActivosPorAlumno(Long alumnoId) {
        List<PrestamoMaterial> prestamos = prestamoRepository
                .findByAlumnoUsuarioIdAndEstadoPrestamoIn(alumnoId, List.of("Activo", "Vencido"));

        return prestamos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> obtenerPrestamosPorEstado(String estado) {
        List<PrestamoMaterial> prestamos = prestamoRepository.findByEstadoPrestamo(estado);
        return prestamos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<PrestamoDTO> obtenerMisPrestamos(Long alumnoId, Pageable pageable) {
        Page<PrestamoMaterial> prestamos = prestamoRepository.findByAlumnoUsuarioId(alumnoId, pageable);
        return prestamos.map(this::mapToDTO);
    }

    @Transactional
    public PrestamoDTO crearPrestamoAlumno(CrearPrestamoDTO prestamoDTO, Long alumnoId) {
        // Obtener alumno
        Usuario alumno = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        // Validar que el usuario es alumno
        if (!"Alumno".equals(alumno.getRol())) {
            throw new BusinessException("Solo los alumnos pueden crear préstamos");
        }

        // Crear DTO para el método existente
        CrearPrestamoDTO crearPrestamoDTO = new CrearPrestamoDTO();
        crearPrestamoDTO.setAlumnoId(alumnoId);
        crearPrestamoDTO.setProductoId(prestamoDTO.getProductoId());
        crearPrestamoDTO.setFechaPrestamo(prestamoDTO.getFechaPrestamo());
        crearPrestamoDTO.setFechaDevolucionEstimada(prestamoDTO.getFechaDevolucionEstimada());
        crearPrestamoDTO.setCantidad(prestamoDTO.getCantidad());
        crearPrestamoDTO.setObservaciones(prestamoDTO.getObservaciones());

        return crearPrestamo(crearPrestamoDTO, alumnoId);
    }

    @Transactional
    public PrestamoDTO devolverPrestamoAlumno(Long prestamoId, Long alumnoId, String observaciones) {
        PrestamoMaterial prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado"));

        // Validar que el préstamo pertenece al alumno
        if (!prestamo.getAlumno().getUsuarioId().equals(alumnoId)) {
            throw new BusinessException("No tiene permisos para devolver este préstamo");
        }

        return devolverPrestamo(prestamoId, observaciones, alumnoId);
    }

    @Transactional
    public void eliminarPrestamo(Long prestamoId, Long usuarioId) {
        PrestamoMaterial prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado"));

        // Solo se pueden eliminar préstamos en estado Activo
        if (!"Activo".equals(prestamo.getEstadoPrestamo())) {
            throw new BusinessException("Solo se pueden eliminar préstamos en estado Activo");
        }

        // Devolver stock si es necesario
        if ("Activo".equals(prestamo.getEstadoPrestamo())) {
            inventarioService.incrementarStock(
                    prestamo.getProducto().getProductoId(),
                    prestamo.getCantidad(),
                    "Préstamo eliminado: " + prestamoId,
                    "PRESTAMO_" + prestamoId,
                    usuarioId);
        }

        prestamoRepository.delete(prestamo);

        log.info("Prestamo eliminado: {} por usuario: {}", prestamoId, usuarioId);

        // Auditoría
        auditoriaService.registrarCambio("PrestamosMateriales", prestamoId, "Eliminar",
                "Préstamo eliminado", null, null, usuarioId, "SYSTEM");
    }

    private PrestamoDTO mapToDTO(PrestamoMaterial prestamo) {
        return PrestamoDTO.builder()
                .prestamoId(prestamo.getPrestamoId())
                .alumnoNombre(prestamo.getAlumno().getNombreCompleto())
                .productoNombre(prestamo.getProducto().getNombre())
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaDevolucionEstimada(prestamo.getFechaDevolucionEstimada())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .estadoPrestamo(prestamo.getEstadoPrestamo())
                .build();
    }
}