package com.api_caich.repository;

import com.api_caich.entity.Ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

        Optional<Ticket> findByFolio(String folio);

        List<Ticket> findByPacienteUsuarioId(Long pacienteId);

        List<Ticket> findByTerapeutaUsuarioId(Long terapeutaId);

        List<Ticket> findByEspacioEspacioId(Long espacioId);

        Page<Ticket> findByPacienteUsuarioId(Long pacienteId, Pageable pageable);

        List<Ticket> findByEstadoTicket(String estadoTicket);

        List<Ticket> findByPacienteUsuarioIdAndEstadoTicket(Long pacienteId, String estadoTicket);

        List<Ticket> findByTerapeutaUsuarioIdAndEstadoTicket(Long terapeutaId, String estadoTicket);

        @Query("SELECT t FROM Ticket t WHERE t.fecha BETWEEN :start AND :end")
        List<Ticket> findByFechaBetween(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // @Query("SELECT t FROM Ticket t WHERE t.estadoTicket = :estado")
        // List<Ticket> findByEstadoTicket(@Param("estado") String estadoTicket);

        @Query("SELECT t FROM Ticket t WHERE t.estadoPago = :estadoPago")
        List<Ticket> findByEstadoPago(@Param("estadoPago") String estadoPago);

        @Query(value = "SELECT COUNT(*) FROM Tickets WHERE CAST(Fecha AS DATE) = CAST(GETDATE() AS DATE)", nativeQuery = true)
        Long countTicketsHoy();

        // @Query("SELECT COUNT(t) FROM Ticket t WHERE YEAR(t.fecha) =
        // YEAR(CURRENT_DATE) AND MONTH(t.fecha) = MONTH(CURRENT_DATE)")
        @Query("SELECT COUNT(t) FROM Ticket t WHERE FUNCTION('YEAR', t.fecha) = FUNCTION('YEAR', CURRENT_DATE) AND FUNCTION('MONTH', t.fecha) = FUNCTION('MONTH', CURRENT_DATE)")
        Long countTicketsEsteMes();

    @Query("""
    SELECT COUNT(t) FROM Ticket t
    WHERE t.espacio.espacioId = :espacioId
    AND t.estadoTicket NOT IN ('Cancelado', 'NoAsistio')
    AND t.fecha < :fechaFin
    AND DATEADD(MINUTE, t.duracion, t.fecha) > :fechaInicio
    AND (:ticketId IS NULL OR t.ticketId != :ticketId)
""")
    Long countConflictingTickets(@Param("espacioId") Long espacioId,
                                 @Param("fechaInicio") LocalDateTime fechaInicio,
                                 @Param("fechaFin") LocalDateTime fechaFin,
                                 @Param("ticketId") Long ticketId);

        @Query("SELECT t FROM Ticket t " +
                        "JOIN FETCH t.paciente p " +
                        "JOIN FETCH t.terapeuta ter " +
                        "LEFT JOIN FETCH t.espacio e " +
                        "WHERE (:pacienteId IS NULL OR t.paciente.usuarioId = :pacienteId) " +
                        "AND (:terapeutaId IS NULL OR t.terapeuta.usuarioId = :terapeutaId) " +
                        "AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket) " +
                        "AND (:estadoPago IS NULL OR t.estadoPago = :estadoPago) " +
                        "AND (:fechaInicio IS NULL OR t.fecha >= :fechaInicio) " +
                        "AND (:fechaFin IS NULL OR t.fecha <= :fechaFin)")
        Page<Ticket> findWithFilters(
                        @Param("pacienteId") Long pacienteId,
                        @Param("terapeutaId") Long terapeutaId,
                        @Param("estadoTicket") String estadoTicket,
                        @Param("estadoPago") String estadoPago,
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin,
                        Pageable pageable);

        @Query("SELECT t FROM Ticket t " +
                        "WHERE t.paciente.usuarioId = :pacienteId " +
                        "AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket) " +
                        "AND (:fechaInicio IS NULL OR t.fecha >= :fechaInicio) " +
                        "AND (:fechaFin IS NULL OR t.fecha <= :fechaFin)")
        Page<Ticket> findByPacienteWithFilters(
                        @Param("pacienteId") Long pacienteId,
                        @Param("estadoTicket") String estadoTicket,
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin,
                        Pageable pageable);

        @Query("SELECT t FROM Ticket t " +
                        "WHERE t.terapeuta.usuarioId = :terapeutaId " +
                        "AND (:estadoTicket IS NULL OR t.estadoTicket = :estadoTicket) " +
                        "AND (:fechaInicio IS NULL OR t.fecha >= :fechaInicio) " +
                        "AND (:fechaFin IS NULL OR t.fecha <= :fechaFin)")
        Page<Ticket> findByTerapeutaWithFilters(
                        @Param("terapeutaId") Long terapeutaId,
                        @Param("estadoTicket") String estadoTicket,
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin,
                        Pageable pageable);

        /**
         * Contar citas conflictivas para un paciente en un horario específico
         */
        @Query("SELECT COUNT(t) FROM Ticket t WHERE " +
                "t.paciente.usuarioId = :pacienteId AND " +
                "t.estadoTicket NOT IN ('Cancelado', 'NoAsistio') AND " +
                // Si la cita existente empieza antes de que la nueva termine
                "t.fecha < :fechaFin AND " +
                // Y la cita existente termina después de que la nueva empiece
                "DATEADD(MINUTE, t.duracion, t.fecha) > :fechaInicio AND " +
                "(:ticketIdExcluir IS NULL OR t.ticketId != :ticketIdExcluir)")
        Long countConflictingTicketsForPaciente(@Param("pacienteId") Long pacienteId,
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin,
                        @Param("ticketIdExcluir") Long ticketIdExcluir);

    /**
     * Contar citas conflictivas para un terapeuta en un horario específico
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE " +
            "t.terapeuta.usuarioId = :terapeutaId AND " +
            "t.estadoTicket NOT IN ('Cancelado', 'NoAsistio') AND " +
            "t.fecha < :fechaFin AND " +
            "DATEADD(MINUTE, t.duracion, t.fecha) > :fechaInicio AND " +
            "(:ticketIdExcluir IS NULL OR t.ticketId != :ticketIdExcluir)")
    Long countConflictingTicketsForTerapeuta(@Param("terapeutaId") Long terapeutaId,
                                             @Param("fechaInicio") LocalDateTime fechaInicio,
                                             @Param("fechaFin") LocalDateTime fechaFin,
                                             @Param("ticketIdExcluir") Long ticketIdExcluir);


    @Query(value = "SELECT ISNULL(MAX(CAST(RIGHT(Folio, 4) AS INT)), 0) " +
            "FROM Tickets " +
            "WHERE Folio LIKE 'TKT-' + FORMAT(GETDATE(), 'yyyyMM') + '-%'",
            nativeQuery = true)
    int findMaxSequenceForCurrentMonth();
}