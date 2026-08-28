package com.api_caich.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Ticket;
import com.api_caich.enums.EstadoFinanciero;
import com.api_caich.enums.EstadoTicket;

/**
 * Repositorio para Ticket con arquitectura modular
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ========== QUERIES BÁSICAS ==========
    /**
     * Buscar ticket por folio único
     */
    Optional<Ticket> findByFolio(String folio);

    /**
     * Buscar tickets creados por un usuario específico
     */
    List<Ticket> findByCreadoPorUsuarioId(Long creadoPorId);

    /**
     * Buscar tickets por estado (enum)
     */
    List<Ticket> findByEstadoTicket(EstadoTicket estadoTicket);

    /**
     * Buscar tickets por estado financiero (enum)
     */
    List<Ticket> findByEstadoFinanciero(EstadoFinanciero estadoFinanciero);

    // ========== QUERIES CON PAGINACIÓN ==========
    /**
     * Buscar tickets por creador con paginación
     */
    Page<Ticket> findByCreadoPorUsuarioId(Long creadoPorId, Pageable pageable);

    // ========== QUERIES DE REPORTES Y CONTADORES ==========
    /**
     * Contar tickets creados hoy
     */
    // TODO: GETDATE() y CAST(x AS DATE) son de SQL Server.
    // Se reemplazó por JPQL portable usando parámetro de rango de fechas.
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.fechaCreacion >= :inicioDia AND t.fechaCreacion < :finDia")
    Long countTicketsHoy(@Param("inicioDia") LocalDateTime inicioDia, @Param("finDia") LocalDateTime finDia);

    /**
     * Contar tickets creados este mes
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE FUNCTION('YEAR', t.fechaCreacion) = FUNCTION('YEAR', CURRENT_DATE) AND FUNCTION('MONTH', t.fechaCreacion) = FUNCTION('MONTH', CURRENT_DATE)")
    Long countTicketsEsteMes();

    /**
     * Buscar tickets por rango de fechas de creación
     */
    @Query("SELECT t FROM Ticket t WHERE t.fechaCreacion BETWEEN :start AND :end")
    List<Ticket> findByFechaCreacionBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ========== QUERIES CON JOINS A CITA ==========
    /**
     * Buscar tickets que tienen cita con un paciente específico
     */
    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN t.cita c WHERE c.paciente.usuarioId = :pacienteId")
    List<Ticket> findTicketsByPaciente(@Param("pacienteId") Long pacienteId);

    /**
     * Buscar tickets que tienen cita con un terapeuta específico
     */
    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN t.cita c WHERE c.terapeuta.usuarioId = :terapeutaId")
    List<Ticket> findTicketsByTerapeuta(@Param("terapeutaId") Long terapeutaId);

    /**
     * Buscar tickets con cita para un paciente con paginación
     */
    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN t.cita c WHERE c.paciente.usuarioId = :pacienteId")
    Page<Ticket> findTicketsByPaciente(@Param("pacienteId") Long pacienteId, Pageable pageable);

    /**
     * Buscar tickets con cita para un terapeuta con paginación
     */
    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN t.cita c WHERE c.terapeuta.usuarioId = :terapeutaId")
    Page<Ticket> findTicketsByTerapeuta(@Param("terapeutaId") Long terapeutaId, Pageable pageable);

    // ========== QUERY DE FILTROS COMPLEJA ==========
    /**
     * Buscar tickets con múltiples filtros (Admin)
     * Incluye tickets con citas Y préstamos de cubículos sin cita
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        LEFT JOIN t.espacios te
        WHERE (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (:estadoFinanciero IS NULL OR t.estadoFinanciero = :estadoFinanciero)
        AND (:pacienteId IS NULL OR c.paciente.usuarioId = :pacienteId)
        AND (:terapeutaId IS NULL OR c.terapeuta.usuarioId = :terapeutaId)
        AND (
            (c IS NOT NULL AND c.fechaInicio >= COALESCE(:fechaInicio, c.fechaInicio))
            OR (c IS NULL AND te.horaInicioReserva >= COALESCE(:fechaInicio, te.horaInicioReserva))
        )
        AND (
            (c IS NOT NULL AND c.fechaInicio <= COALESCE(:fechaFin, c.fechaInicio))
            OR (c IS NULL AND te.horaInicioReserva <= COALESCE(:fechaFin, te.horaInicioReserva))
        )
    """)
    Page<Ticket> findWithFilters(
            @Param("pacienteId") Long pacienteId,
            @Param("terapeutaId") Long terapeutaId,
            @Param("estadoTicket") EstadoTicket estadoTicket,
            @Param("estadoFinanciero") EstadoFinanciero estadoFinanciero,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    /**
     * Buscar tickets de un paciente con filtros
     * Incluye citas donde es paciente Y préstamos de cubículos que creó
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        LEFT JOIN t.espacios te
        LEFT JOIN t.comprador comprador
        WHERE (
            c.paciente.usuarioId = :pacienteId
            OR comprador.usuarioId = :pacienteId
            OR (c IS NULL AND t.creadoPor.usuarioId = :pacienteId)
        )
        AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (COALESCE(c.fechaInicio, te.horaInicioReserva, t.fechaCreacion) >= COALESCE(:fechaInicio, COALESCE(c.fechaInicio, te.horaInicioReserva, t.fechaCreacion)))
        AND (COALESCE(c.fechaInicio, te.horaInicioReserva, t.fechaCreacion) <= COALESCE(:fechaFin, COALESCE(c.fechaInicio, te.horaInicioReserva, t.fechaCreacion)))
    """)
    Page<Ticket> findByPacienteWithFilters(
            @Param("pacienteId") Long pacienteId,
            @Param("estadoTicket") EstadoTicket estadoTicket,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    /**
     * Buscar tickets de un terapeuta con filtros
     * Incluye citas donde es terapeuta Y préstamos de cubículos que creó
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        LEFT JOIN t.espacios te
        WHERE (
            c.terapeuta.usuarioId = :terapeutaId
            OR (c IS NULL AND t.creadoPor.usuarioId = :terapeutaId)
        )
        AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (
            (c IS NOT NULL AND c.fechaInicio >= COALESCE(:fechaInicio, c.fechaInicio))
            OR (c IS NULL AND te.horaInicioReserva >= COALESCE(:fechaInicio, te.horaInicioReserva))
        )
        AND (
            (c IS NOT NULL AND c.fechaInicio <= COALESCE(:fechaFin, c.fechaInicio))
            OR (c IS NULL AND te.horaInicioReserva <= COALESCE(:fechaFin, te.horaInicioReserva))
        )
    """)
    Page<Ticket> findByTerapeutaWithFilters(
            @Param("terapeutaId") Long terapeutaId,
            @Param("estadoTicket") EstadoTicket estadoTicket,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    /**
     * Buscar tickets de un alumno con filtros
     * Incluye solo tickets (reservas de cubículos) que creó el alumno
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.espacios te
        LEFT JOIN t.comprador comprador
        WHERE (t.creadoPor.usuarioId = :alumnoId OR comprador.usuarioId = :alumnoId)
        AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (COALESCE(te.horaInicioReserva, t.fechaCreacion) >= COALESCE(:fechaInicio, COALESCE(te.horaInicioReserva, t.fechaCreacion)))
        AND (COALESCE(te.horaInicioReserva, t.fechaCreacion) <= COALESCE(:fechaFin, COALESCE(te.horaInicioReserva, t.fechaCreacion)))
    """)
    Page<Ticket> findByAlumnoWithFilters(
            @Param("alumnoId") Long alumnoId,
            @Param("estadoTicket") EstadoTicket estadoTicket,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    // ========== GENERACIÓN DE FOLIOS ==========
    /**
     * Obtiene todos los folios del año actual para calcular la siguiente secuencia
     * en Java y evitar dependencias del motor de base de datos.
     */
    @Query("SELECT t.folio FROM Ticket t WHERE t.folio LIKE CONCAT(:yearPrefix, '%')")
    List<String> findFoliosByYearPrefix(@Param("yearPrefix") String yearPrefix);

    // ========== CONSULTAS PARA DISPONIBILIDAD DE HORARIOS ==========
    /**
     * Buscar citas de un espacio en un rango de fechas para validar disponibilidad
     */
    @Query("""
        SELECT c FROM TicketCita c 
        JOIN c.ticket t 
        JOIN t.espacios te 
        WHERE te.espacio.espacioId = :espacioId
        AND c.fechaInicio < :fechaFin 
        AND c.fechaFin > :fechaInicio
        AND t.estadoTicket != 'CANCELADO'
        ORDER BY c.fechaInicio
    """)
    List<com.api_caich.entity.TicketCita> findCitasByEspacioAndFechaRange(
            @Param("espacioId") Long espacioId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

}

