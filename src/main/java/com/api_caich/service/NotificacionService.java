package com.api_caich.service;

import com.api_caich.dto.notificacion.NotificacionDTO;
import com.api_caich.entity.Notificacion;
import com.api_caich.entity.Usuario;
import com.api_caich.repository.NotificacionRepository;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public void notificarCitaProxima(Long pacienteId, Long ticketId, LocalDateTime fecha) {
        Usuario paciente = usuarioRepository.findById(pacienteId).orElse(null);
        if (paciente == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(paciente);
        notificacion.setTipo("CitaProxima");
        notificacion.setTitulo("Cita Próxima");
        notificacion.setMensaje(String.format("Tienes una cita programada para el %s",
                fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        notificacion.setReferenciaId(ticketId);
        notificacion.setReferenciaUrl("/tickets/" + ticketId);

        notificacionRepository.save(notificacion);
        log.info("Cita proxima notification created for user: {}", pacienteId);
    }

    public void notificarPagoVerificado(Long pacienteId, Long ticketId, String folio) {
        Usuario paciente = usuarioRepository.findById(pacienteId).orElse(null);
        if (paciente == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(paciente);
        notificacion.setTipo("PagoVerificado");
        notificacion.setTitulo("Pago Verificado");
        notificacion.setMensaje(String.format("Tu pago para el ticket %s ha sido verificado exitosamente", folio));
        notificacion.setReferenciaId(ticketId);
        notificacion.setReferenciaUrl("/tickets/" + ticketId);

        notificacionRepository.save(notificacion);
    }

    public void notificarPagoRechazado(Long pacienteId, Long ticketId, String folio, String motivo) {
        Usuario paciente = usuarioRepository.findById(pacienteId).orElse(null);
        if (paciente == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(paciente);
        notificacion.setTipo("PagoRechazado");
        notificacion.setTitulo("Pago Rechazado");
        notificacion.setMensaje(String.format("Tu pago para el ticket %s fue rechazado. Motivo: %s", folio, motivo));
        notificacion.setReferenciaId(ticketId);
        notificacion.setReferenciaUrl("/tickets/" + ticketId);

        notificacionRepository.save(notificacion);
    }

    public void notificarMaterialVencido(Long alumnoId, Long prestamoId, String productoNombre, int diasVencidos) {
        Usuario alumno = usuarioRepository.findById(alumnoId).orElse(null);
        if (alumno == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(alumno);
        notificacion.setTipo("MaterialVencido");
        notificacion.setTitulo("Material Vencido");
        notificacion.setMensaje(String.format("El material '%s' está vencido por %d días. Por favor devuélvelo.",
                productoNombre, diasVencidos));
        notificacion.setReferenciaId(prestamoId);
        notificacion.setReferenciaUrl("/prestamos/" + prestamoId);

        notificacionRepository.save(notificacion);
    }

    public void notificarMaterialPerdido(Long prestamoId, String productoNombre, String alumnoNombre) {
        // Notificar a todos los administradores
        List<Usuario> administradores = usuarioRepository.findByRol("Administrador");

        for (Usuario admin : administradores) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(admin);
            notificacion.setTipo("MaterialPerdido");
            notificacion.setTitulo("Material Perdido");
            notificacion.setMensaje(String.format("El material '%s' fue marcado como perdido por el alumno %s",
                    productoNombre, alumnoNombre));
            notificacion.setReferenciaId(prestamoId);
            notificacion.setReferenciaUrl("/prestamos/" + prestamoId);

            notificacionRepository.save(notificacion);
        }
    }

    public void notificarStockBajo(Long productoId, String productoNombre, int stockActual) {
        List<Usuario> administradores = usuarioRepository.findByRol("Administrador");

        // Evitar duplicados
        boolean notificacionExistente = notificacionRepository.existsByTipoAndReferenciaIdAndLeidaFalse(
                "StockBajo", productoId);

        if (notificacionExistente) {
            return;
        }

        for (Usuario admin : administradores) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(admin);
            notificacion.setTipo("StockBajo");
            notificacion.setTitulo("Stock Bajo");
            notificacion.setMensaje(String.format("El producto '%s' tiene stock bajo. Stock actual: %d",
                    productoNombre, stockActual));
            notificacion.setReferenciaId(productoId);
            notificacion.setReferenciaUrl("/inventario/productos/" + productoId);

            notificacionRepository.save(notificacion);
        }

        log.info("Stock bajo notification created for product: {}", productoNombre);
    }

    public void notificarCitaAgendada(Long pacienteId, Long ticketId, LocalDateTime fecha,
            String espacioNombre, String terapeutaNombre) {
        try {
            // Aquí puedes implementar:
            // - Email
            // - SMS (usando el celular del ticket)
            // - Notificación push
            // - Mensaje interno en la app

            log.info("Notificacion de cita agendada enviada - Paciente: {}, Ticket: {}, Fecha: {}",
                    pacienteId, ticketId, fecha);

        } catch (Exception e) {
            log.error("Error enviando notificacion de cita agendada", e);
            // No lanzar excepción para no afectar el flujo principal
        }
    }

    public void notificarCitaCancelada(Long pacienteId, Long ticketId, String folio) {
        try {
            log.info("Notificacion de cita cancelada - Paciente: {}, Ticket: {}, Folio: {}",
                    pacienteId, ticketId, folio);
            // Implementar notificación por email/SMS
        } catch (Exception e) {
            log.error("Error enviando notificacion de cancelacion", e);
        }
    }

    public void notificarCancelacionCita(Long pacienteId, Long ticketId, String folio, String motivo) {
        Usuario paciente = usuarioRepository.findById(pacienteId).orElse(null);
        if (paciente == null)
            return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(paciente);
        notificacion.setTipo("CitaCancelada");
        notificacion.setTitulo("Cita Cancelada");
        notificacion.setMensaje(String.format("Tu cita (Folio: %s) ha sido cancelada. Motivo: %s", folio, motivo));
        notificacion.setReferenciaId(ticketId);
        notificacion.setReferenciaUrl("/tickets/" + ticketId);

        notificacionRepository.save(notificacion);
        log.info("Cita cancelada notification created for user: {}", pacienteId);
    }

    public void notificarCambioCita(Long pacienteId, Long ticketId, String cambios) {
        try {
            log.info("Notificacion de cambios en cita - Paciente: {}, Ticket: {}, Cambios: {}",
                    pacienteId, ticketId, cambios);
            // Implementar notificación por email/SMS
        } catch (Exception e) {
            log.error("Error enviando notificacion de cambios", e);
        }
    }

    public void notificarCitaEliminada(Long pacienteId, Long ticketId, String folio) {
        try {
            log.info("Notificacion de cita eliminada - Paciente: {}, Ticket: {}, Folio: {}",
                    pacienteId, ticketId, folio);
            // Implementar notificación por email/SMS
        } catch (Exception e) {
            log.error("Error enviando notificacion de eliminacion", e);
        }
    }

    @Transactional
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        // Verificar que la notificación pertenece al usuario
        if (!notificacion.getUsuario().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No autorizado para marcar esta notificación");
        }

        notificacion.setLeida(true);
        notificacion.setFechaLeida(LocalDateTime.now());
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public int marcarTodasComoLeidas(Long usuarioId) {
        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioUsuarioIdAndLeidaFalse(usuarioId);

        for (Notificacion notificacion : noLeidas) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
        }

        notificacionRepository.saveAll(noLeidas);
        return noLeidas.size();
    }

    public List<NotificacionDTO> obtenerNoLeidas(Long usuarioId) {
        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);

        return notificaciones.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificacionDTO> obtenerTodas(Long usuarioId, int limite) {
        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioUsuarioIdOrderByFechaCreacionDesc(usuarioId);

        if (limite > 0) {
            notificaciones = notificaciones.stream().limit(limite).collect(Collectors.toList());
        }

        return notificaciones.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void limpiarNotificacionesAntiguas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
        int eliminadas = notificacionRepository.deleteByLeidaTrueAndFechaLeidaBefore(fechaLimite);

        log.info("Cleaned up {} old notifications", eliminadas);
    }

    private NotificacionDTO mapToDTO(Notificacion notificacion) {
        return NotificacionDTO.builder()
                .notificacionId(notificacion.getNotificacionId())
                .tipo(notificacion.getTipo())
                .titulo(notificacion.getTitulo())
                .mensaje(notificacion.getMensaje())
                .leida(notificacion.getLeida())
                .fechaCreacion(notificacion.getFechaCreacion())
                .referenciaUrl(notificacion.getReferenciaUrl())
                .build();
    }
}