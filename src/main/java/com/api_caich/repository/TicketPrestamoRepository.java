package com.api_caich.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.TicketPrestamo;
import com.api_caich.enums.EstadoPrestamo;

@Repository
public interface TicketPrestamoRepository extends JpaRepository<TicketPrestamo, Long> {

    List<TicketPrestamo> findByTicket_TicketId(Long ticketId);

    List<TicketPrestamo> findByResponsable_UsuarioId(Long responsableId);

    List<TicketPrestamo> findByEstado(String estado);

    Page<TicketPrestamo> findByEstado(EstadoPrestamo estado, Pageable pageable);
}
