package com.api_caich.service;

import com.api_caich.dto.cita.ActualizarCitaDTO;
import com.api_caich.dto.cita.CitaDTO;
import com.api_caich.dto.cita.CrearCitaDTO;
import com.api_caich.entity.*;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.*;
import com.api_caich.util.FolioGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private final TicketRepository ticketRepository;
    private final EspacioRepository espacioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final TicketDetalleRepository ticketDetalleRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final PrestamoMaterialRepository prestamoMaterialRepository;
    private final ComprobanteService comprobanteService;

    public List<CitaDTO> obtenerCitasConFiltros(Long espacioId, Long terapeutaId, Long pacienteId, String estado) {
        // Usar Pageable.unpaged() para obtener todos los resultados sin paginación
        Page<Ticket> ticketsPage = ticketRepository.findWithFilters(
                pacienteId, terapeutaId, estado, null, null, null, Pageable.unpaged());

        List<Ticket> tickets = ticketsPage.getContent();

        return tickets.stream()
                .map(this::mapToCitaDTO)
                .collect(Collectors.toList());
    }

    public List<CitaDTO> obtenerCitasPorPaciente(Long pacienteId, String estado) {
        List<Ticket> tickets;
        if (estado != null) {
            tickets = ticketRepository.findByPacienteUsuarioIdAndEstadoTicket(pacienteId, estado);
        } else {
            tickets = ticketRepository.findByPacienteUsuarioId(pacienteId);
        }

        return tickets.stream()
                .map(this::mapToCitaDTO)
                .collect(Collectors.toList());
    }

    public List<CitaDTO> obtenerCitasPorTerapeuta(Long terapeutaId, String estado) {
        List<Ticket> tickets;
        if (estado != null) {
            tickets = ticketRepository.findByTerapeutaUsuarioIdAndEstadoTicket(terapeutaId, estado);
        } else {
            tickets = ticketRepository.findByTerapeutaUsuarioId(terapeutaId);
        }

        return tickets.stream()
                .map(this::mapToCitaDTO)
                .collect(Collectors.toList());
    }

    public List<CitaDTO> obtenerCitasPorEstado(String estado) {
        List<Ticket> tickets = ticketRepository.findByEstadoTicket(estado);
        return tickets.stream()
                .map(this::mapToCitaDTO)
                .collect(Collectors.toList());
    }

    public CitaDTO obtenerCitaPorId(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + ticketId));
        return mapToCitaDTO(ticket);
    }

    @Transactional
    public CitaDTO crearCita(CrearCitaDTO crearCitaDTO, Long usuarioId) {
        log.info("Creando cita con creacion automatica de pago pendiente - Transactional");

        // Validar disponibilidad del espacio
        validarDisponibilidadEspacio(crearCitaDTO.getEspacioId(), crearCitaDTO.getFecha(),
                crearCitaDTO.getDuracion(), null);

        // Obtener entidades relacionadas
        Espacio espacio = espacioRepository.findById(crearCitaDTO.getEspacioId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

        Usuario paciente = usuarioRepository.findById(crearCitaDTO.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Usuario terapeuta = usuarioRepository.findById(crearCitaDTO.getTerapeutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Terapeuta no encontrado"));

        CuentaBancaria cuentaDestino = cuentaBancariaRepository.findById(crearCitaDTO.getCuentaDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));

        Usuario creadoPor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        // Validar que el terapeuta tenga rol de terapeuta
        if (!"Terapeuta".equals(terapeuta.getRol())) {
            throw new BusinessException("El usuario especificado no es un terapeuta");
        }

        // VALIDAR QUE EL PACIENTE NO TENGA OTRA CITA EN EL MISMO HORARIO
        validarDisponibilidadPaciente(crearCitaDTO.getPacienteId(), crearCitaDTO.getFecha(),
                crearCitaDTO.getDuracion(), null);

        // VALIDAR QUE EL TERAPEUTA NO TENGA OTRA CITA EN EL MISMO HORARIO
        validarDisponibilidadTerapeuta(crearCitaDTO.getTerapeutaId(), crearCitaDTO.getFecha(),
                crearCitaDTO.getDuracion(), null);

        // Generar folio único
        String folio = FolioGenerator.generarFolioTicket();

        // CALCULAR MONTO AUTOMÁTICAMENTE según la lógica requerida por frontend
        BigDecimal montoCalculado = calcularMontoCita(crearCitaDTO, espacio);

        // Crear ticket (cita) con pago pendiente automático
        Ticket ticket = new Ticket();
        ticket.setFolio(folio);
        ticket.setEspacio(espacio);
        ticket.setFecha(crearCitaDTO.getFecha());
        ticket.setDuracion(crearCitaDTO.getDuracion());
        ticket.setPaciente(paciente);
        ticket.setTerapeuta(terapeuta);
        ticket.setCuentaDestino(cuentaDestino);
        ticket.setMateria(crearCitaDTO.getMateria());
        ticket.setModalidad(crearCitaDTO.getModalidad());
        ticket.setMontoPagado(BigDecimal.ZERO); // Inicialmente 0, se paga después
        ticket.setEstadoTicket("Agendado");
        ticket.setEstadoPago("Pendiente"); // PAGO PENDIENTE CREADO AUTOMATICAMENTE
        ticket.setCostoEspacio(montoCalculado); // Usar el monto calculado como costo del espacio
        ticket.setCostoMateriales(BigDecimal.ZERO);
        ticket.setCostoAdicional(BigDecimal.ZERO);
        ticket.setCostoTotal(montoCalculado); // Total = monto calculado
        ticket.setCreadoPor(creadoPor);
        ticket.setNotas(crearCitaDTO.getNotas());

        // AGREGAR CELULAR SI ESTA PRESENTE EN EL DTO
        if (crearCitaDTO.getCelular() != null && !crearCitaDTO.getCelular().trim().isEmpty()) {
            ticket.setCelular(crearCitaDTO.getCelular());
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // CREAR NOTIFICACION PARA EL PACIENTE
        notificacionService.notificarCitaAgendada(
                paciente.getUsuarioId(),
                savedTicket.getTicketId(),
                savedTicket.getFecha(),
                espacio.getNombre(),
                terapeuta.getNombreCompleto());

        log.info("Cita creada con pago pendiente automatico - Ticket: {} - Fecha: {} - Materia: {} - Monto: {}",
                savedTicket.getTicketId(), savedTicket.getFecha(),
                savedTicket.getMateria(), montoCalculado);

        // Auditoría
        auditoriaService.registrarCambio("Tickets", savedTicket.getTicketId(), "Crear",
                "Cita creada con pago pendiente automático. Monto calculado: " + montoCalculado,
                null, null, usuarioId, "SYSTEM");

        return mapToCitaDTO(savedTicket);
    }

    @Transactional
    public CitaDTO actualizarCita(Long ticketId, ActualizarCitaDTO actualizarCitaDTO, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + ticketId));

        // Validar que la cita no esté completada o cancelada
        if ("Completado".equals(ticket.getEstadoTicket()) || "Cancelado".equals(ticket.getEstadoTicket())) {
            throw new BusinessException("No se puede modificar una cita en estado: " + ticket.getEstadoTicket());
        }

        String cambios = "";

        // VALIDAR DISPONIBILIDAD PARA CAMBIOS DE FECHA/HORA
        if (actualizarCitaDTO.getFecha() != null || actualizarCitaDTO.getDuracion() != null) {
            LocalDateTime nuevaFecha = actualizarCitaDTO.getFecha() != null ? actualizarCitaDTO.getFecha()
                    : ticket.getFecha();
            Integer nuevaDuracion = actualizarCitaDTO.getDuracion() != null ? actualizarCitaDTO.getDuracion()
                    : ticket.getDuracion();
            Long espacioId = actualizarCitaDTO.getEspacioId() != null ? actualizarCitaDTO.getEspacioId()
                    : ticket.getEspacio().getEspacioId();

            validarDisponibilidadEspacio(espacioId, nuevaFecha, nuevaDuracion, ticketId);

            // Validar disponibilidad del paciente
            Long pacienteId = actualizarCitaDTO.getPacienteId() != null ? actualizarCitaDTO.getPacienteId()
                    : ticket.getPaciente().getUsuarioId();
            validarDisponibilidadPaciente(pacienteId, nuevaFecha, nuevaDuracion, ticketId);

            // Validar disponibilidad del terapeuta
            Long terapeutaId = actualizarCitaDTO.getTerapeutaId() != null ? actualizarCitaDTO.getTerapeutaId()
                    : ticket.getTerapeuta().getUsuarioId();
            validarDisponibilidadTerapeuta(terapeutaId, nuevaFecha, nuevaDuracion, ticketId);
        }

        // Validar y actualizar espacio si es diferente
        if (actualizarCitaDTO.getEspacioId() != null
                && !actualizarCitaDTO.getEspacioId().equals(ticket.getEspacio().getEspacioId())) {
            Espacio nuevoEspacio = espacioRepository.findById(actualizarCitaDTO.getEspacioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado"));

            cambios += "Espacio: " + ticket.getEspacio().getNombre() + " -> " + nuevoEspacio.getNombre() + "; ";
            ticket.setEspacio(nuevoEspacio);
        }

        // Actualizar otros campos
        if (actualizarCitaDTO.getFecha() != null) {
            cambios += "Fecha: " + ticket.getFecha() + " -> " + actualizarCitaDTO.getFecha() + "; ";
            ticket.setFecha(actualizarCitaDTO.getFecha());
        }

        if (actualizarCitaDTO.getDuracion() != null) {
            cambios += "Duracion: " + ticket.getDuracion() + " -> " + actualizarCitaDTO.getDuracion() + "; ";
            ticket.setDuracion(actualizarCitaDTO.getDuracion());

            // RECALCULAR COSTOS SI CAMBIA LA DURACION
            if (ticket.getEspacio() != null) {
                //TODO: Revisar esta funcion calcularCostoPorDuracion
                BigDecimal nuevoCosto = calcularCostoPorDuracion(actualizarCitaDTO.getDuracion(),
                        ticket.getEspacio().getCostoPorHora());
                cambios += "CostoEspacio: " + ticket.getCostoEspacio() + " -> " + nuevoCosto + "; ";
                ticket.setCostoEspacio(nuevoCosto);
                ticket.setCostoTotal(nuevoCosto.add(ticket.getCostoMateriales()).add(ticket.getCostoAdicional()));
            }
        }

        if (actualizarCitaDTO.getPacienteId() != null) {
            Usuario nuevoPaciente = usuarioRepository.findById(actualizarCitaDTO.getPacienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            cambios += "Paciente: " + ticket.getPaciente().getNombreCompleto() + " -> "
                    + nuevoPaciente.getNombreCompleto() + "; ";
            ticket.setPaciente(nuevoPaciente);
        }

        if (actualizarCitaDTO.getTerapeutaId() != null) {
            Usuario nuevoTerapeuta = usuarioRepository.findById(actualizarCitaDTO.getTerapeutaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Terapeuta no encontrado"));

            if (!"Terapeuta".equals(nuevoTerapeuta.getRol())) {
                throw new BusinessException("El usuario especificado no es un terapeuta");
            }

            cambios += "Terapeuta: " + ticket.getTerapeuta().getNombreCompleto() + " -> "
                    + nuevoTerapeuta.getNombreCompleto() + "; ";
            ticket.setTerapeuta(nuevoTerapeuta);
        }

        if (actualizarCitaDTO.getMateria() != null) {
            cambios += "Materia: " + ticket.getMateria() + " -> " + actualizarCitaDTO.getMateria() + "; ";
            ticket.setMateria(actualizarCitaDTO.getMateria());
        }

        if (actualizarCitaDTO.getModalidad() != null) {
            cambios += "Modalidad: " + ticket.getModalidad() + " -> " + actualizarCitaDTO.getModalidad() + "; ";
            ticket.setModalidad(actualizarCitaDTO.getModalidad());
        }

        // MANEJO ESPECIAL PARA CAMBIO DE ESTADO A "CANCELADO"
        boolean seCancelo = false;
        if (actualizarCitaDTO.getEstadoTicket() != null) {
            String estadoAnterior = ticket.getEstadoTicket();
            cambios += "EstadoTicket: " + estadoAnterior + " -> " + actualizarCitaDTO.getEstadoTicket() + "; ";
            ticket.setEstadoTicket(actualizarCitaDTO.getEstadoTicket());

            // Si se cancela la cita, actualizar el estado del pago
            if ("Cancelado".equals(actualizarCitaDTO.getEstadoTicket()) && !"Cancelado".equals(estadoAnterior)) {
                seCancelo = true;
                cambios += "EstadoPago: " + ticket.getEstadoPago() + " -> Cancelado; ";
                ticket.setEstadoPago("Cancelado");

                // Notificar cancelación
                notificacionService.notificarCitaCancelada(
                        ticket.getPaciente().getUsuarioId(),
                        ticketId,
                        ticket.getFolio());
            }
        }

        if (actualizarCitaDTO.getNotas() != null) {
            String notasActualizadas = ticket.getNotas() != null ? ticket.getNotas() + "\n--- ACTUALIZACIÓN "
                    + LocalDateTime.now() + " ---\n" + actualizarCitaDTO.getNotas() : actualizarCitaDTO.getNotas();
            cambios += "Notas actualizadas; ";
            ticket.setNotas(notasActualizadas);
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Cita actualizada: {} - Cambios: {}", updatedTicket.getTicketId(), cambios);

        // NOTIFICAR CAMBIOS IMPORTANTES
        if (cambios.contains("Fecha") || cambios.contains("Espacio")) {
            notificacionService.notificarCambioCita(
                    ticket.getPaciente().getUsuarioId(),
                    ticketId,
                    cambios);
        }

        // Auditoría si hubo cambios
        if (!cambios.isEmpty()) {
            auditoriaService.registrarCambio("Tickets", ticketId, "Actualizar",
                    cambios, null, null, usuarioId, "SYSTEM");
        }

        return mapToCitaDTO(updatedTicket);
    }

    @Transactional
    public void eliminarCita(Long ticketId, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + ticketId));

        // Solo permitir eliminar citas agendadas
        if (!"Agendado".equals(ticket.getEstadoTicket())) {
            throw new BusinessException("Solo se pueden eliminar citas en estado 'Agendado'");
        }

        // VALIDAR QUE NO TENGA PAGOS ASOCIADOS (excepto pendientes o cancelados)
        if ("Pagado".equals(ticket.getEstadoPago()) || "Verificado".equals(ticket.getEstadoPago())) {
            throw new BusinessException("No se puede eliminar una cita con pagos " + ticket.getEstadoPago() +
                    ". Primero cancele el pago.");
        }

        // VALIDAR QUE NO TENGA PRESTAMOS DE MATERIALES ACTIVOS
        if (ticket.getPrestamos() != null && !ticket.getPrestamos().isEmpty()) {
            boolean tienePrestamosActivos = ticket.getPrestamos().stream()
                    .anyMatch(prestamo -> "Activo".equals(prestamo.getEstadoPrestamo()) ||
                            "Vencido".equals(prestamo.getEstadoPrestamo()));

            if (tienePrestamosActivos) {
                throw new BusinessException("No se puede eliminar una cita con préstamos de materiales activos");
            }
        }

        // ELIMINAR DETALLES DEL TICKET (PRODUCTOS ASOCIADOS)
        if (ticket.getDetalles() != null && !ticket.getDetalles().isEmpty()) {
            ticketDetalleRepository.deleteAll(ticket.getDetalles());
            log.info("Eliminados {} detalles del ticket {}", ticket.getDetalles().size(), ticketId);
        }

        // DEVOLVER STOCK DE PRODUCTOS SI ES NECESARIO
        if (ticket.getDetalles() != null) {
            for (TicketDetalle detalle : ticket.getDetalles()) {
                if (("Venta".equals(detalle.getTipoUso()) || "Uso".equals(detalle.getTipoUso())) &&
                        detalle.getProducto() != null) {

                    inventarioService.incrementarStock(
                            detalle.getProducto().getProductoId(),
                            detalle.getCantidad(),
                            "Devolución por eliminación de cita: " + ticket.getFolio(),
                            "TICKET_DELETE_" + ticketId,
                            usuarioId);
                }
            }
        }

        // ELIMINAR PRESTAMOS ASOCIADOS (si existen)
        if (ticket.getPrestamos() != null && !ticket.getPrestamos().isEmpty()) {
            prestamoMaterialRepository.deleteAll(ticket.getPrestamos());
            log.info("Eliminados {} prestamos asociados al ticket {}", ticket.getPrestamos().size(), ticketId);
        }

        //ELIMINAR COMPROBANTES ASOCIADOS (si existen)
        // comprobanteService.eliminarComprobantesPorTicket(ticketId);

        ticketRepository.delete(ticket);

        log.info("Cita eliminada completamente: {} - Folio: {}", ticketId, ticket.getFolio());

        // NOTIFICAR ELIMINACION
        notificacionService.notificarCitaEliminada(
                ticket.getPaciente().getUsuarioId(),
                ticketId,
                ticket.getFolio());

        // Auditoría
        auditoriaService.registrarCambio("Tickets", ticketId, "Eliminar",
                "Cita eliminada completamente. Folio: " + ticket.getFolio(), null, null, usuarioId, "SYSTEM");
    }

    private void validarDisponibilidadEspacio(Long espacioId, LocalDateTime fecha, Integer duracion,
            Long ticketIdExcluir) {
        if (espacioId == null || fecha == null || duracion == null) {
            return;
        }

        LocalDateTime fechaFin = fecha.plusMinutes(duracion);

        Long conflictCount = ticketRepository.countConflictingTickets(espacioId, fecha, fechaFin, ticketIdExcluir);

        if (conflictCount > 0) {
            throw new BusinessException("El espacio no está disponible en el horario seleccionado");
        }
    }

    // Mapper de Ticket a CitaDTO
    private CitaDTO mapToCitaDTO(Ticket ticket) {
        return CitaDTO.builder()
                .ticketId(ticket.getTicketId())
                .espacioId(ticket.getEspacio() != null ? ticket.getEspacio().getEspacioId() : null)
                .espacioNombre(ticket.getEspacio() != null ? ticket.getEspacio().getNombre() : null)
                .fecha(ticket.getFecha())
                .duracion(ticket.getDuracion())
                .pacienteId(ticket.getPaciente().getUsuarioId())
                .pacienteNombre(ticket.getPaciente().getNombreCompleto())
                .terapeutaId(ticket.getTerapeuta().getUsuarioId())
                .terapeutaNombre(ticket.getTerapeuta().getNombreCompleto())
                .materia(ticket.getMateria())
                .modalidad(ticket.getModalidad())
                .estadoTicket(ticket.getEstadoTicket())
                .notas(ticket.getNotas())
                .fechaCreacion(ticket.getFechaCreacion())
                .build();
    }

    /**
     * CALCULAR MONTO AUTOMATICAMENTE segun la logica requerida por frontend
     * Lógica: Si montoPagado es 0 o null, calcular automáticamente basado en
     * duración y costo del espacio
     */
    private BigDecimal calcularMontoCita(CrearCitaDTO crearCitaDTO, Espacio espacio) {
        // Si el frontend envía un montoPagado > 0, usarlo directamente
        if (crearCitaDTO.getMontoPagado() != null &&
                crearCitaDTO.getMontoPagado().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Usando monto proporcionado por frontend: {}", crearCitaDTO.getMontoPagado());
            return crearCitaDTO.getMontoPagado();
        }

        // CALCULAR AUTOMATICAMENTE basado en duracion y costo por hora del espacio
        BigDecimal costoPorHora = espacio.getCostoPorHora();

        // Validar que el espacio tenga costo configurado
        if (costoPorHora == null || costoPorHora.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Espacio {} no tiene costoPorHora configurado, usando valor por defecto: 100",
                    espacio.getNombre());
            costoPorHora = BigDecimal.valueOf(100); // Valor por defecto
        }

        // Convertir duración de minutos a horas (con decimales)
        double horas = crearCitaDTO.getDuracion() / 60.0;
        BigDecimal montoCalculado = costoPorHora.multiply(BigDecimal.valueOf(horas));

        // Redondear a 2 decimales
        montoCalculado = montoCalculado.setScale(2, RoundingMode.HALF_UP);

        log.info("Monto calculado automaticamente: {} (costo/hora) * {} horas = {}",
                costoPorHora, String.format("%.2f", horas), montoCalculado);

        return montoCalculado;
    }

    /**
     * VALIDAR QUE EL PACIENTE NO TENGA OTRA CITA EN EL MISMO HORARIO
     */
    private void validarDisponibilidadPaciente(Long pacienteId, LocalDateTime fechaInicio,
            Integer duracion, Long ticketIdExcluir) {
        if (pacienteId == null || fechaInicio == null || duracion == null) {
            return;
        }

        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        Long conflictCount = ticketRepository.countConflictingTicketsForPaciente(
                pacienteId, fechaInicio, fechaFin, ticketIdExcluir);

        if (conflictCount > 0) {
            throw new BusinessException("El paciente ya tiene una cita agendada en este horario");
        }
    }

    /**
     * VALIDAR QUE EL TERAPEUTA NO TENGA OTRA CITA EN EL MISMO HORARIO
     */
    private void validarDisponibilidadTerapeuta(Long terapeutaId, LocalDateTime fechaInicio,
            Integer duracion, Long ticketIdExcluir) {
        if (terapeutaId == null || fechaInicio == null || duracion == null) {
            return;
        }

        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        Long conflictCount = ticketRepository.countConflictingTicketsForTerapeuta(
                terapeutaId, fechaInicio, fechaFin, ticketIdExcluir);

        if (conflictCount > 0) {
            throw new BusinessException("El terapeuta ya tiene una cita agendada en este horario");
        }
    }

    /**
     * CALCULAR COSTO POR DURACION
     * Calcula el costo basado en la duración en minutos y el costo por hora
     */
    private BigDecimal calcularCostoPorDuracion(Integer duracionMinutos, BigDecimal costoPorHora) {
        if (duracionMinutos == null || costoPorHora == null) {
            return BigDecimal.ZERO;
        }

        // Convertir duración de minutos a horas (con decimales)
        double horas = duracionMinutos / 60.0;
        BigDecimal montoCalculado = costoPorHora.multiply(BigDecimal.valueOf(horas));

        // Redondear a 2 decimales
        montoCalculado = montoCalculado.setScale(2, RoundingMode.HALF_UP);

        return montoCalculado;
    }

}