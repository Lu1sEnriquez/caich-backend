package com.api_caich.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.TicketEspacio;
import com.api_caich.enums.EstadoTicket;

@Repository
public interface TicketEspacioRepository extends JpaRepository<TicketEspacio, Long> {

    List<TicketEspacio> findByTicket_TicketId(Long ticketId);

    List<TicketEspacio> findByEspacio_EspacioId(Long espacioId);

    List<TicketEspacio> findByHoraInicioReservaBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Contar reservas de espacio conflictivas para un espacio en un horario
     * específico
     */
    @Query("""
        SELECT COUNT(te) FROM TicketEspacio te 
        WHERE te.espacio.espacioId = :espacioId 
        AND te.ticket.estadoTicket NOT IN (:estadosExcluidos)
        AND te.horaInicioReserva < :horaFin 
        AND te.horaFinReserva > :horaInicio 
        AND (:reservaIdExcluir IS NULL OR te.id != :reservaIdExcluir)
    """)
    Long countConflictingReservas(
            @Param("espacioId") Long espacioId,
            @Param("horaInicio") LocalDateTime horaInicio,
            @Param("horaFin") LocalDateTime horaFin,
            @Param("estadosExcluidos") List<EstadoTicket> estadosExcluidos,
            @Param("reservaIdExcluir") Long reservaIdExcluir
    );
}
