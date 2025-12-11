package com.api_caich.repository;

import com.api_caich.entity.TicketDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketDetalleRepository extends JpaRepository<TicketDetalle, Long> {

    List<TicketDetalle> findByTicketTicketId(Long ticketId);

    List<TicketDetalle> findByProductoProductoId(Long productoId);
}