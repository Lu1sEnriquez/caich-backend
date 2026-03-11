package com.api_caich.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Pago;
import com.api_caich.entity.Ticket;
import com.api_caich.enums.EstadoPagoTransaccion;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByTicket_TicketId(Long ticketId);

    List<Pago> findByEstado(EstadoPagoTransaccion estado);

    List<Pago> findByTicketAndEstado(Ticket ticket, EstadoPagoTransaccion estado);

    /**
     * Verifica si ya existe un pago sincronizado con un identificador específico en las notas.
     * Se usa para detección de duplicados en la sincronización de Google Forms.
     */
    boolean existsByNotasContaining(String syncKey);
}
