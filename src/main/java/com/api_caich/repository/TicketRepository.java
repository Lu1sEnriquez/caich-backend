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
    @Query(value = "SELECT COUNT(*) FROM Tickets WHERE CAST(FechaCreacion AS DATE) = CAST(GETDATE() AS DATE)", nativeQuery = true)
    Long countTicketsHoy();

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
     * Buscar tickets con múltiples filtros (Admin) Incluye filtros por
     * paciente/terapeuta (via cita), estados, fechas
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        WHERE (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (:estadoFinanciero IS NULL OR t.estadoFinanciero = :estadoFinanciero)
        AND (:pacienteId IS NULL OR c.paciente.usuarioId = :pacienteId)
        AND (:terapeutaId IS NULL OR c.terapeuta.usuarioId = :terapeutaId)
        AND (:fechaInicio IS NULL OR t.fechaCreacion >= :fechaInicio)
        AND (:fechaFin IS NULL OR t.fechaCreacion <= :fechaFin)
        ORDER BY t.fechaCreacion DESC
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
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        WHERE c.paciente.usuarioId = :pacienteId
        AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (:fechaInicio IS NULL OR t.fechaCreacion >= :fechaInicio)
        AND (:fechaFin IS NULL OR t.fechaCreacion <= :fechaFin)
        ORDER BY t.fechaCreacion DESC
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
     */
    @Query("""
        SELECT DISTINCT t FROM Ticket t 
        LEFT JOIN t.cita c 
        WHERE c.terapeuta.usuarioId = :terapeutaId
        AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket)
        AND (:fechaInicio IS NULL OR t.fechaCreacion >= :fechaInicio)
        AND (:fechaFin IS NULL OR t.fechaCreacion <= :fechaFin)
        ORDER BY t.fechaCreacion DESC
    """)
    Page<Ticket> findByTerapeutaWithFilters(
            @Param("terapeutaId") Long terapeutaId,
            @Param("estadoTicket") EstadoTicket estadoTicket,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    // ========== GENERACIÓN DE FOLIOS ==========
    /**
     * Obtener el número de secuencia más alto del mes actual para generar
     * folios
     */
    @Query(value = "SELECT ISNULL(MAX(CAST(RIGHT(Folio, 4) AS INT)), 0) "
            + "FROM Tickets "
            + "WHERE Folio LIKE 'TKT-' + FORMAT(GETDATE(), 'yyyyMM') + '-%'",
            nativeQuery = true)
    int findMaxSequenceForCurrentMonth();
}
