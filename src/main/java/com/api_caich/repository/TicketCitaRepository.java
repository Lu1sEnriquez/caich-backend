package com.api_caich.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.TicketCita;
import com.api_caich.enums.EstadoTicket;

@Repository
public interface TicketCitaRepository extends JpaRepository<TicketCita, Long> {

    Optional<TicketCita> findByTicket_TicketId(Long ticketId);

    List<TicketCita> findByTerapeuta_UsuarioId(Long terapeutaId);

    List<TicketCita> findByPaciente_UsuarioId(Long pacienteId);

    List<TicketCita> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Contar citas conflictivas para un paciente en un horario específico
     * Verifica solapamiento: cita existente empieza antes de que la nueva
     * termine Y cita existente termina después de que la nueva empiece
     */
    @Query("""
        SELECT COUNT(c) FROM TicketCita c 
        WHERE c.paciente.usuarioId = :pacienteId 
        AND c.ticket.estadoTicket NOT IN (:estadosExcluidos)
        AND c.fechaInicio < :fechaFin 
        AND c.fechaFin > :fechaInicio 
        AND (:citaIdExcluir IS NULL OR c.id != :citaIdExcluir)
    """)
    Long countConflictingCitasForPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("estadosExcluidos") List<EstadoTicket> estadosExcluidos,
            @Param("citaIdExcluir") Long citaIdExcluir
    );

    /**
     * Contar citas conflictivas para un terapeuta en un horario específico
     */
    @Query("""
        SELECT COUNT(c) FROM TicketCita c 
        WHERE c.terapeuta.usuarioId = :terapeutaId 
        AND c.ticket.estadoTicket NOT IN (:estadosExcluidos)
        AND c.fechaInicio < :fechaFin 
        AND c.fechaFin > :fechaInicio 
        AND (:citaIdExcluir IS NULL OR c.id != :citaIdExcluir)
    """)
    Long countConflictingCitasForTerapeuta(
            @Param("terapeutaId") Long terapeutaId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("estadosExcluidos") List<EstadoTicket> estadosExcluidos,
            @Param("citaIdExcluir") Long citaIdExcluir
    );
}
