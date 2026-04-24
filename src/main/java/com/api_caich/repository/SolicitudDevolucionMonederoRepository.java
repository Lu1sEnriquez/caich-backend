package com.api_caich.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.SolicitudDevolucionMonedero;
import com.api_caich.enums.EstadoDevolucionMonedero;

@Repository
public interface SolicitudDevolucionMonederoRepository extends JpaRepository<SolicitudDevolucionMonedero, Long> {

    List<SolicitudDevolucionMonedero> findByEstadoOrderByFechaSolicitudAsc(EstadoDevolucionMonedero estado);

    Optional<SolicitudDevolucionMonedero> findByTicket_TicketId(Long ticketId);

    boolean existsByTicket_TicketId(Long ticketId);

    long countByEstado(EstadoDevolucionMonedero estado);
}
