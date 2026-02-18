package com.api_caich.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.TicketVenta;

@Repository
public interface TicketVentaRepository extends JpaRepository<TicketVenta, Long> {

    List<TicketVenta> findByTicket_TicketId(Long ticketId);
}
