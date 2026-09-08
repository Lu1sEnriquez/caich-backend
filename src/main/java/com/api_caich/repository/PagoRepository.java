package com.api_caich.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.Pago;
import com.api_caich.entity.Ticket;
import com.api_caich.enums.EstadoPagoTransaccion;
import com.api_caich.enums.MetodoPago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByTicket_TicketId(Long ticketId);

    List<Pago> findByEstado(EstadoPagoTransaccion estado);

    Page<Pago> findByMetodoPagoNot(MetodoPago metodoPago, Pageable pageable);

    Page<Pago> findByEstadoAndMetodoPagoNot(
            EstadoPagoTransaccion estado,
            MetodoPago metodoPago,
            Pageable pageable);

    List<Pago> findByTicketAndEstado(Ticket ticket, EstadoPagoTransaccion estado);

    /**
     * Verifica si ya existe un pago sincronizado con un identificador específico en las notas.
     * Se usa para detección de duplicados en la sincronización de Google Forms.
     */
    boolean existsByNotasContaining(String syncKey);
}
